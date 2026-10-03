package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.support.IntegrationTestBase;
import com.ensar.clmp.support.TestDataFactory;

import tools.jackson.databind.JsonNode;

/** US4 — Market a Ready consultant (AS 4.1–4.8, FR-040–FR-045, FR-032/036 marketing triggers). */
class US4MarketingIT extends IntegrationTestBase {

    private Long readyConsultantOf(String recruiterUser, String first) {
        return data.consultant(TestDataFactory.completeProfile(first, "Marketer", "Java"), ConsultantStatus.READY,
                data.recruiterIdOfUser(recruiterUser));
    }

    private JsonNode create(String user, Long consultantId, String start, String target) throws Exception {
        return body(postAs(user, "/api/marketing-assignments",
                map("consultantId", consultantId, "startDate", start, "targetDate", target))
                .andExpect(status().isCreated()));
    }

    private JsonNode transition(String user, long id, String target, String reason) throws Exception {
        long version = body(getAs("admin", "/api/marketing-assignments/{id}", id)).get("version").asLong();
        return body(postAs(user, "/api/marketing-assignments/{id}/transition",
                map("targetStatus", target, "reason", reason, "version", version), id).andExpect(status().isOk()));
    }

    private long consultantVersion(Long id) throws Exception {
        return body(getAs("admin", "/api/consultants/{id}", id)).get("version").asLong();
    }

    @Test
    void as4_1_createForReadyConsultantIsDraftOwnedByRecruiterAndTeam() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Ada");
        JsonNode created = create("recruiter1", c, "2026-10-15", "2026-11-15");
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");
        assertThat(created.get("ownerRecruiter").get("fullName").asText()).isEqualTo("Riya Patel");
        assertThat(created.get("team").get("name").asText()).isEqualTo("Java");
        assertThat(created.get("startDate").asText()).isEqualTo("2026-10-15");
        assertThat(created.get("targetDate").asText()).isEqualTo("2026-11-15");
        assertThat(created.get("consultant").get("id").asLong()).isEqualTo(c);
        assertThat(created.get("overdue").asBoolean()).isFalse();
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.READY);
    }

    @Test
    void fr040_onlyReadyConsultantsCanBeMarketed() throws Exception {
        Long riya = data.recruiterIdOfUser("recruiter1");
        for (ConsultantStatus s : new ConsultantStatus[] { ConsultantStatus.BENCH, ConsultantStatus.MARKETING,
                ConsultantStatus.INTERVIEWING, ConsultantStatus.HOLD, ConsultantStatus.INACTIVE }) {
            Long c = data.consultant(TestDataFactory.completeProfile("Not", "Eligible", "Go"), s, riya);
            expectProblem(postAs("admin", "/api/marketing-assignments",
                    map("consultantId", c, "startDate", "2026-10-15", "targetDate", "2026-11-15")), 422,
                    "CONSULTANT_NOT_ELIGIBLE").andExpect(jsonPath("$.consultantStatus").value(s.name()));
        }
    }

    @Test
    void as4_2_activationMakesConsultantMarketingWithSystemTriggeredHistory() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Bea");
        long id = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
        JsonNode active = transition("recruiter1", id, "ACTIVE", null);
        assertThat(active.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.MARKETING);

        getAs("recruiter1", "/api/marketing-assignments/{id}/history", id)
                .andExpect(jsonPath("$.items[0].changeType").value("STATUS"))
                .andExpect(jsonPath("$.items[0].oldValue").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].newValue").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].actor").value("Riya Patel"))
                .andExpect(jsonPath("$.items[0].systemTriggered").value(false));
        getAs("admin", "/api/consultants/{id}/history", c)
                .andExpect(jsonPath("$.items[0].oldValue").value("READY"))
                .andExpect(jsonPath("$.items[0].newValue").value("MARKETING"))
                .andExpect(jsonPath("$.items[0].systemTriggered").value(true))
                .andExpect(jsonPath("$.items[0].trigger.event").value("MARKETING_ACTIVATED"))
                .andExpect(jsonPath("$.items[0].trigger.entityType").value("MARKETING_ASSIGNMENT"))
                .andExpect(jsonPath("$.items[0].trigger.entityId").value(id))
                .andExpect(jsonPath("$.items[0].actor").value("Riya Patel"))
                .andExpect(jsonPath("$.items[0].occurredAt").value("2026-10-15T15:00:00Z"));
    }

    @Test
    void as4_3_activationRefusedWhenConsultantNoLongerReady() throws Exception {
        for (ConsultantStatus status : new ConsultantStatus[] { ConsultantStatus.BENCH, ConsultantStatus.HOLD,
                ConsultantStatus.INACTIVE }) {
            Long c = readyConsultantOf("recruiter1", "Cy");
            long id = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
            String reason = status == ConsultantStatus.BENCH ? null : "Because";
            postAs("hr", "/api/consultants/{id}/status",
                    map("targetStatus", status.name(), "reason", reason, "version", consultantVersion(c)), c)
                    .andExpect(status().isOk());
            JsonNode detail = body(getAs("admin", "/api/marketing-assignments/{id}", id));
            if (!"DRAFT".equals(detail.get("status").asText())) {
                continue; // Inactive closes the draft; nothing left to activate.
            }
            expectProblem(postAs("recruiter1", "/api/marketing-assignments/{id}/transition",
                    map("targetStatus", "ACTIVE", "version", detail.get("version").asLong()), id), 422,
                    "CONSULTANT_NOT_ELIGIBLE")
                    .andExpect(jsonPath("$.consultantStatus").value(status.name()))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Ready")));
        }
    }

    @Test
    void as4_4_secondOpenAssignmentIsRefusedWithReference() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Dee");
        long first = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
        expectProblem(postAs("recruiter1", "/api/marketing-assignments",
                map("consultantId", c, "startDate", "2026-10-16", "targetDate", "2026-11-16")), 409,
                "OPEN_ASSIGNMENT_EXISTS").andExpect(jsonPath("$.existingRecordId").value(first));

        transition("recruiter1", first, "ACTIVE", null);
        expectProblem(postAs("admin", "/api/marketing-assignments",
                map("consultantId", c, "startDate", "2026-10-16", "targetDate", "2026-11-16")), 409,
                "OPEN_ASSIGNMENT_EXISTS").andExpect(jsonPath("$.existingRecordId").value(first));
    }

    @Test
    void as4_5_and_4_6_holdReopenCloseAndConsultantReturnsToReady() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Eve");
        long id = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
        transition("recruiter1", id, "ACTIVE", null);

        expectProblem(postAs("recruiter1", "/api/marketing-assignments/{id}/transition",
                map("targetStatus", "HOLD", "version", body(getAs("admin", "/api/marketing-assignments/{id}", id))
                        .get("version").asLong()), id), 400, "VALIDATION_FAILED");

        JsonNode held = transition("recruiter1", id, "HOLD", "Client budget freeze");
        assertThat(held.get("status").asText()).isEqualTo("HOLD");
        assertThat(held.get("holdReason").asText()).isEqualTo("Client budget freeze");
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.MARKETING);

        assertThat(transition("recruiter1", id, "ACTIVE", null).get("status").asText()).isEqualTo("ACTIVE");

        JsonNode closed = transition("recruiter1", id, "CLOSED", "Consultant asked to pause");
        assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
        assertThat(closed.get("closeReason").asText()).isEqualTo("Consultant asked to pause");
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.READY);
        getAs("admin", "/api/consultants/{id}/history", c)
                .andExpect(jsonPath("$.items[0].trigger.event").value("MARKETING_CLOSED"))
                .andExpect(jsonPath("$.items[0].newValue").value("READY"));
        getAs("admin", "/api/marketing-assignments/{id}/history", id)
                .andExpect(jsonPath("$.items[0].reason").value("Consultant asked to pause"));
    }

    @Test
    void as4_7_recruiterSeesOwnAssignmentsAndOverdueFlag() throws Exception {
        Long mine = readyConsultantOf("recruiter1", "Fay");
        Long theirs = readyConsultantOf("recruiter2", "Gus");
        long overdue = create("recruiter1", mine, "2026-09-01", "2026-10-14").get("id").asLong();
        long other = create("recruiter2", theirs, "2026-10-01", "2026-12-01").get("id").asLong();

        JsonNode page = body(getAs("recruiter1", "/api/marketing-assignments?size=100"));
        java.util.Set<Long> ids = new java.util.HashSet<>();
        page.get("items").forEach(i -> ids.add(i.get("id").asLong()));
        assertThat(ids).contains(overdue).doesNotContain(other);
        getAs("recruiter1", "/api/marketing-assignments/{id}", overdue).andExpect(jsonPath("$.overdue").value(true));
        getAs("recruiter1", "/api/marketing-assignments?overdue=true&size=100")
                .andExpect(jsonPath("$.items[?(@.id == " + overdue + ")]").exists());
        expectProblem(getAs("recruiter1", "/api/marketing-assignments/{id}", other), 403, "NOT_AUTHORIZED");

        // Target date equal to today is not overdue; closed is never overdue.
        Long today = readyConsultantOf("recruiter1", "Hal");
        assertThat(create("recruiter1", today, "2026-10-01", "2026-10-15").get("overdue").asBoolean()).isFalse();
        transition("admin", overdue, "CLOSED", null);
        getAs("recruiter1", "/api/marketing-assignments/{id}", overdue).andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void as4_8_managerSeesAllAndMayOnlyHoldReopenClose() throws Exception {
        Long c = readyConsultantOf("recruiter2", "Ivy");
        long id = create("recruiter2", c, "2026-10-15", "2026-11-15").get("id").asLong();

        getAs("manager", "/api/marketing-assignments?size=100")
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").exists());
        JsonNode draft = body(getAs("manager", "/api/marketing-assignments/{id}", id));
        assertThat(draft.get("allowedTransitions").toString()).isEqualTo("[\"CLOSED\"]");
        expectProblem(postAs("manager", "/api/marketing-assignments/{id}/transition",
                map("targetStatus", "ACTIVE", "version", draft.get("version").asLong()), id), 403, "NOT_AUTHORIZED");

        transition("recruiter2", id, "ACTIVE", null);
        assertThat(transition("manager", id, "HOLD", "Manager hold").get("status").asText()).isEqualTo("HOLD");
        assertThat(transition("manager", id, "ACTIVE", null).get("status").asText()).isEqualTo("ACTIVE");
        assertThat(transition("manager", id, "CLOSED", "Manager close").get("status").asText()).isEqualTo("CLOSED");
        assertThat(transition("manager", id, "ACTIVE", null).get("status").asText()).isEqualTo("ACTIVE");
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.MARKETING);

        expectProblem(postAs("manager", "/api/marketing-assignments",
                map("consultantId", readyConsultantOf("recruiter2", "Jo"), "startDate", "2026-10-15", "targetDate",
                        "2026-11-15")), 403, "NOT_AUTHORIZED");
        expectProblem(putAs("manager", "/api/marketing-assignments/{id}",
                map("startDate", "2026-10-15", "targetDate", "2026-12-15", "version", 0), id), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("manager", "/api/marketing-assignments/{id}/notes", map("body", "hi"), id), 403,
                "NOT_AUTHORIZED");
    }

    @Test
    void recruiterCannotReopenClosed() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Kai");
        long id = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
        transition("recruiter1", id, "CLOSED", null);
        JsonNode closed = body(getAs("recruiter1", "/api/marketing-assignments/{id}", id));
        assertThat(closed.get("allowedTransitions")).isEmpty();
        expectProblem(postAs("recruiter1", "/api/marketing-assignments/{id}/transition",
                map("targetStatus", "ACTIVE", "version", closed.get("version").asLong()), id), 403, "NOT_AUTHORIZED");
    }

    @Test
    void datesAndNotes() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Lou");
        expectProblem(postAs("recruiter1", "/api/marketing-assignments",
                map("consultantId", c, "startDate", "2026-10-15", "targetDate", "2026-10-14")), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("targetDate"));

        long id = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
        putAs("recruiter1", "/api/marketing-assignments/{id}",
                map("startDate", "2026-10-16", "targetDate", "2026-12-01", "version", 0), id)
                .andExpect(status().isOk()).andExpect(jsonPath("$.targetDate").value("2026-12-01"));
        expectProblem(putAs("recruiter1", "/api/marketing-assignments/{id}",
                map("startDate", "2026-12-16", "targetDate", "2026-12-01", "version", 1), id), 400, "VALIDATION_FAILED");

        postAs("recruiter1", "/api/marketing-assignments/{id}/notes", map("body", "Sent resume to 3 vendors"), id)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("Riya Patel"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-15T15:00:00Z"));
        expectProblem(postAs("recruiter1", "/api/marketing-assignments/{id}/notes", map("body", "  "), id), 400,
                "VALIDATION_FAILED");
        getAs("recruiter1", "/api/marketing-assignments/{id}", id)
                .andExpect(jsonPath("$.notes.length()").value(1))
                .andExpect(jsonPath("$.notes[0].body").value("Sent resume to 3 vendors"));

        // Notes are append-only: no update or delete mapping exists.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/marketing-assignments/{id}/notes/1", id).session(loginAs("recruiter1"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/marketing-assignments/{id}/notes/1", id).session(loginAs("recruiter1"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
    }

    @Test
    void consultantHoldCascadesToActiveAssignment() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Max");
        long id = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
        transition("recruiter1", id, "ACTIVE", null);
        postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "HOLD", "reason", "Medical leave", "version", consultantVersion(c)), c)
                .andExpect(status().isOk());
        getAs("admin", "/api/marketing-assignments/{id}", id).andExpect(jsonPath("$.status").value("HOLD"));
        getAs("admin", "/api/marketing-assignments/{id}/history", id)
                .andExpect(jsonPath("$.items[0].systemTriggered").value(true))
                .andExpect(jsonPath("$.items[0].trigger.event").value("CONSULTANT_HOLD"))
                .andExpect(jsonPath("$.items[0].trigger.entityType").value("CONSULTANT"))
                .andExpect(jsonPath("$.items[0].trigger.entityId").value(c))
                .andExpect(jsonPath("$.items[0].actor").value("Harper HR"));
    }

    @Test
    void consultantInactiveClosesOpenAssignment() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Ned");
        long id = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
        transition("recruiter1", id, "ACTIVE", null);
        postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "INACTIVE", "reason", "Left", "version", consultantVersion(c)), c)
                .andExpect(status().isOk());
        getAs("admin", "/api/marketing-assignments/{id}", id)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closeReason").value("Consultant inactive"));
        getAs("admin", "/api/marketing-assignments/{id}/history", id)
                .andExpect(jsonPath("$.items[0].trigger.event").value("CONSULTANT_INACTIVE"));
    }

    @Test
    void reassignmentTransfersOwnershipAndNewRecruiterCanUpdate() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Oli");
        long id = create("recruiter1", c, "2026-10-15", "2026-11-15").get("id").asLong();
        transition("recruiter1", id, "ACTIVE", null);
        Long marcus = data.recruiterIdOfUser("recruiter2");
        postAs("manager", "/api/consultants/{id}/recruiter", map("recruiterId", marcus, "version", consultantVersion(c)),
                c).andExpect(status().isOk());

        getAs("admin", "/api/marketing-assignments/{id}", id)
                .andExpect(jsonPath("$.ownerRecruiter.fullName").value("Marcus Lee"))
                .andExpect(jsonPath("$.team.name").value("Data & Analytics"));
        getAs("admin", "/api/marketing-assignments/{id}/history", id)
                .andExpect(jsonPath("$.items[0].changeType").value("OWNER_TRANSFER"))
                .andExpect(jsonPath("$.items[0].oldValue").value("Riya Patel"))
                .andExpect(jsonPath("$.items[0].newValue").value("Marcus Lee"));
        postAs("recruiter2", "/api/marketing-assignments/{id}/notes", map("body", "Picked this up"), id)
                .andExpect(status().isCreated());
        expectProblem(getAs("recruiter1", "/api/marketing-assignments/{id}", id), 403, "NOT_AUTHORIZED");
    }

    @Test
    void consultantDetailCarriesMarketingPanelExceptForHr() throws Exception {
        Long c = readyConsultantOf("recruiter1", "Pia");
        long id = create("recruiter1", c, "2026-09-01", "2026-10-01").get("id").asLong();
        getAs("recruiter1", "/api/consultants/{id}", c)
                .andExpect(jsonPath("$.currentMarketingAssignment.id").value(id))
                .andExpect(jsonPath("$.currentMarketingAssignment.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentMarketingAssignment.overdue").value(true));
        getAs("hr", "/api/consultants/{id}", c).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMarketingAssignment").doesNotExist());
        expectProblem(getAs("hr", "/api/marketing-assignments/{id}", id), 403, "NOT_AUTHORIZED");
    }

    @Test
    void filtersByStatusRecruiterAndTeam() throws Exception {
        Long c = readyConsultantOf("recruiter2", "Quin");
        long id = create("recruiter2", c, "2026-10-15", "2026-11-15").get("id").asLong();
        Long marcus = data.recruiterIdOfUser("recruiter2");
        JsonNode byRecruiter = body(getAs("admin", "/api/marketing-assignments?status=DRAFT&recruiterId=" + marcus
                + "&size=100"));
        java.util.Set<Long> ids = new java.util.HashSet<>();
        byRecruiter.get("items").forEach(i -> {
            ids.add(i.get("id").asLong());
            assertThat(i.get("ownerRecruiter").get("id").asLong()).isEqualTo(marcus);
            assertThat(i.get("status").asText()).isEqualTo("DRAFT");
        });
        assertThat(ids).contains(id);
        long teamId = body(getAs("admin", "/api/marketing-assignments/{id}", id)).get("team").get("id").asLong();
        getAs("admin", "/api/marketing-assignments?teamId=" + teamId + "&size=100")
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").exists());
        getAs("admin", "/api/marketing-assignments?sort=targetDate,desc").andExpect(status().isOk());
    }
}
