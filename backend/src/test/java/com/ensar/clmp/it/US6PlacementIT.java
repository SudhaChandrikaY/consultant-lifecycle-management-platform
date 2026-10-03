package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.support.IntegrationTestBase;
import com.ensar.clmp.support.TestDataFactory;

import tools.jackson.databind.JsonNode;

/** US6 — Convert an offer into a placement (AS 6.1–6.7, FR-070–FR-077). */
class US6PlacementIT extends IntegrationTestBase {

    @MockitoSpyBean
    HistoryService historySpy;

    private Long consultant(String recruiterUser, String first) {
        return data.consultant(TestDataFactory.completeProfile(first, "Placed", "Java"), ConsultantStatus.READY,
                data.recruiterIdOfUser(recruiterUser));
    }

    private long submission(String user, Long consultantId, String client, String submittedDate) throws Exception {
        return body(postAs(user, "/api/submissions", map("consultantId", consultantId, "vendorName", "Pl Vendor",
                "clientName", client, "jobTitle", "Java Developer", "billRate", 90, "submitNow", true,
                "submittedDate", submittedDate)).andExpect(status().isCreated())).get("id").asLong();
    }

    private void move(String user, long id, String... targets) throws Exception {
        for (String t : targets) {
            long v = body(getAs("admin", "/api/submissions/{id}", id)).get("version").asLong();
            postAs(user, "/api/submissions/{id}/status", map("targetStatus", t, "version", v), id)
                    .andExpect(status().isOk());
        }
    }

    private long offer(String user, Long consultantId, String client) throws Exception {
        long id = submission(user, consultantId, client, "2026-10-01");
        move(user, id, "INTERVIEW_SCHEDULED", "INTERVIEW_CLEARED", "OFFER");
        return id;
    }

    private Map<String, Object> placement(long submissionId, String startDate) {
        return map("submissionId", submissionId, "startDate", startDate, "billRate", 95.5, "contractTermMonths", 12);
    }

    private long consultantVersion(Long id) throws Exception {
        return body(getAs("admin", "/api/consultants/{id}", id)).get("version").asLong();
    }

    @Test
    void as6_1_and_6_3_draftIsPrefilledAndOnlyFromOffer() throws Exception {
        Long c = consultant("recruiter1", "Dru");
        long notOffer = submission("recruiter1", c, "Draft Client", "2026-10-01");
        expectProblem(getAs("recruiter1", "/api/placements/draft?submissionId=" + notOffer), 422,
                "SUBMISSION_NOT_AT_OFFER").andExpect(jsonPath("$.submissionStatus").value("SUBMITTED"));
        expectProblem(postAs("recruiter1", "/api/placements", placement(notOffer, "2026-11-01")), 422,
                "SUBMISSION_NOT_AT_OFFER");

        long id = offer("recruiter1", c, "Draft Client 2");
        getAs("recruiter1", "/api/placements/draft?submissionId=" + id).andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(id))
                .andExpect(jsonPath("$.consultant.id").value(c))
                .andExpect(jsonPath("$.recruiter.fullName").value("Riya Patel"))
                .andExpect(jsonPath("$.vendor.name").value("Pl Vendor"))
                .andExpect(jsonPath("$.client.name").value("Draft Client 2"))
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"))
                .andExpect(jsonPath("$.billRate").value(90.0))
                .andExpect(jsonPath("$.submittedDate").value("2026-10-01"));
    }

    @Test
    void as6_2_and_6_4_createChangesEverythingOnceAndListsOtherOpenSubmissions() throws Exception {
        Long c = consultant("recruiter1", "Pat");
        long ma = body(postAs("recruiter1", "/api/marketing-assignments",
                map("consultantId", c, "startDate", "2026-10-01", "targetDate", "2026-11-01"))
                .andExpect(status().isCreated())).get("id").asLong();
        postAs("recruiter1", "/api/marketing-assignments/{id}/transition", map("targetStatus", "ACTIVE", "version", 0),
                ma).andExpect(status().isOk());
        long other = submission("recruiter1", c, "Other Client", "2026-10-02");
        long source = offer("recruiter1", c, "Main Client");

        JsonNode created = body(postAs("recruiter1", "/api/placements", placement(source, "2026-11-02"))
                .andExpect(status().isCreated()));
        long placementId = created.get("id").asLong();
        assertThat(created.get("recruiter").get("fullName").asText()).isEqualTo("Riya Patel");
        assertThat(created.get("expectedEndDate").asText()).isEqualTo("2027-11-02");
        assertThat(created.get("billRate").asDouble()).isEqualTo(95.5);
        assertThat(created.get("otherOpenSubmissions")).hasSize(1);
        assertThat(created.get("otherOpenSubmissions").get(0).get("id").asLong()).isEqualTo(other);

        getAs("admin", "/api/submissions/{id}", source).andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.timeline[0].trigger.event").value("PLACEMENT_CREATED"))
                .andExpect(jsonPath("$.timeline[0].trigger.entityId").value(placementId));
        getAs("admin", "/api/submissions/{id}", other).andExpect(jsonPath("$.status").value("SUBMITTED"));
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.PLACED);
        getAs("admin", "/api/consultants/{id}/history", c)
                .andExpect(jsonPath("$.items[0].newValue").value("PLACED"))
                .andExpect(jsonPath("$.items[0].systemTriggered").value(true))
                .andExpect(jsonPath("$.items[0].trigger.event").value("PLACEMENT_CREATED"))
                .andExpect(jsonPath("$.items[0].trigger.entityType").value("PLACEMENT"))
                .andExpect(jsonPath("$.items[0].trigger.entityId").value(placementId))
                .andExpect(jsonPath("$.items[0].actor").value("Riya Patel"))
                .andExpect(jsonPath("$.items[0].description").value("Placement at Main Client via Pl Vendor"));
        getAs("admin", "/api/marketing-assignments/{id}", ma)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closeReason").value("Placed"))
                .andExpect(jsonPath("$.allowedTransitions").isEmpty());
        getAs("admin", "/api/marketing-assignments/{id}/history", ma)
                .andExpect(jsonPath("$.items[0].trigger.event").value("PLACEMENT_CREATED"));
        expectProblem(postAs("admin", "/api/marketing-assignments/{id}/transition",
                map("targetStatus", "ACTIVE", "version", body(getAs("admin", "/api/marketing-assignments/{id}", ma))
                        .get("version").asLong()), ma), 422, "BUSINESS_RULE");
    }

    @Test
    void as6_5_activeProjectOnlyFromStartDate() throws Exception {
        Long future = consultant("recruiter1", "Fut");
        postAs("recruiter1", "/api/placements", placement(offer("recruiter1", future, "F Client"), "2026-10-16"))
                .andExpect(status().isCreated());
        expectProblem(postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "ACTIVE_PROJECT", "version", consultantVersion(future)), future), 422,
                "BUSINESS_RULE");

        Long today = consultant("recruiter1", "Tod");
        postAs("recruiter1", "/api/placements", placement(offer("recruiter1", today, "T Client"), "2026-10-15"))
                .andExpect(status().isCreated());
        postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "ACTIVE_PROJECT", "version", consultantVersion(today)), today)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE_PROJECT"));
    }

    @Test
    void as6_6_and_6_7_visibilityByRole() throws Exception {
        Long c = consultant("recruiter1", "Vis");
        long pid = body(postAs("recruiter1", "/api/placements", placement(offer("recruiter1", c, "Vis Client"),
                "2026-11-01")).andExpect(status().isCreated())).get("id").asLong();

        getAs("recruiter1", "/api/placements/{id}", pid).andExpect(status().isOk());
        getAs("recruiter1", "/api/placements?size=100").andExpect(jsonPath("$.items[?(@.id == " + pid + ")]").exists());
        getAs("recruiter2", "/api/placements?size=100")
                .andExpect(jsonPath("$.items[?(@.id == " + pid + ")]").doesNotExist());
        expectProblem(getAs("recruiter2", "/api/placements/{id}", pid), 403, "NOT_AUTHORIZED");
        getAs("manager", "/api/placements/{id}", pid).andExpect(status().isOk());

        expectProblem(getAs("hr", "/api/placements"), 403, "NOT_AUTHORIZED");
        expectProblem(getAs("hr", "/api/placements/{id}", pid), 403, "NOT_AUTHORIZED");
        String detail = getAs("hr", "/api/consultants/{id}", c).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).doesNotContain("\"placements\"").doesNotContain("\"submissions\"")
                .doesNotContain("vendor").doesNotContain("client").doesNotContain("Client")
                .doesNotContain("billRate");
        String history = getAs("hr", "/api/consultants/{id}/history", c)
                .andExpect(jsonPath("$.items[0].description").value("Placement created"))
                .andExpect(jsonPath("$.items[0].trigger.entityType").isEmpty())
                .andExpect(jsonPath("$.items[0].trigger.entityId").isEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(history).doesNotContain("Vis Client").doesNotContain("Pl Vendor");

        getAs("recruiter1", "/api/consultants/{id}", c)
                .andExpect(jsonPath("$.placements[0].id").value(pid))
                .andExpect(jsonPath("$.placements[0].clientName").value("Vis Client"))
                .andExpect(jsonPath("$.placements[0].startDate").value("2026-11-01"));
    }

    @Test
    void fr070_onlyAdminOrTheSubmissionsRecruiterMayPlace() throws Exception {
        // recruiter2 submits; the consultant is then reassigned to recruiter1.
        Long c = consultant("recruiter2", "Own");
        long sub = offer("recruiter2", c, "Own Client");
        postAs("manager", "/api/consultants/{id}/recruiter",
                map("recruiterId", data.recruiterIdOfUser("recruiter1"), "version", consultantVersion(c)), c)
                .andExpect(status().isOk());

        expectProblem(getAs("recruiter1", "/api/placements/draft?submissionId=" + sub), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("recruiter1", "/api/placements", placement(sub, "2026-11-01")), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("manager", "/api/placements", placement(sub, "2026-11-01")), 403, "NOT_AUTHORIZED");
        expectProblem(getAs("manager", "/api/placements/draft?submissionId=" + sub), 403, "NOT_AUTHORIZED");

        long pid = body(postAs("recruiter2", "/api/placements", placement(sub, "2026-11-01"))
                .andExpect(status().isCreated())).get("id").asLong();
        getAs("recruiter2", "/api/placements/{id}", pid).andExpect(jsonPath("$.recruiter.fullName").value("Marcus Lee"));

        // ADMIN can place anyone's offer; the recruiter is still the submission's recruiter.
        Long c2 = consultant("recruiter2", "Adm");
        long sub2 = offer("recruiter2", c2, "Admin Client");
        postAs("admin", "/api/placements", placement(sub2, "2026-11-01")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.recruiter.fullName").value("Marcus Lee"));
        getAs("manager", "/api/placements?size=100").andExpect(jsonPath("$.totalItems").isNumber());
    }

    @Test
    void holdConsultantMayBePlacedFromExistingOffer() throws Exception {
        Long c = consultant("recruiter1", "Hol");
        long ma = body(postAs("recruiter1", "/api/marketing-assignments",
                map("consultantId", c, "startDate", "2026-10-01", "targetDate", "2026-11-01"))
                .andExpect(status().isCreated())).get("id").asLong();
        postAs("recruiter1", "/api/marketing-assignments/{id}/transition", map("targetStatus", "ACTIVE", "version", 0),
                ma).andExpect(status().isOk());
        long sub = offer("recruiter1", c, "Hold Client");
        postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "HOLD", "reason", "Paperwork", "version", consultantVersion(c)), c)
                .andExpect(status().isOk());
        getAs("admin", "/api/marketing-assignments/{id}", ma).andExpect(jsonPath("$.status").value("HOLD"));

        postAs("recruiter1", "/api/placements", placement(sub, "2026-11-01")).andExpect(status().isCreated());
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.PLACED);
        getAs("admin", "/api/consultants/{id}/history", c).andExpect(jsonPath("$.items[0].oldValue").value("HOLD"));
        getAs("admin", "/api/marketing-assignments/{id}", ma).andExpect(jsonPath("$.closeReason").value("Placed"));

        Long other = data.consultant(TestDataFactory.completeProfile("Still", "Held", "Go"), ConsultantStatus.HOLD,
                data.recruiterIdOfUser("recruiter1"));
        expectProblem(postAs("recruiter1", "/api/submissions", map("consultantId", other, "vendorName", "V",
                "clientName", "C", "jobTitle", "J", "billRate", 10, "submitNow", true)), 422, "CONSULTANT_NOT_ELIGIBLE");
    }

    @Test
    void fr075_noSecondPlacementAndDateAndTermRules() throws Exception {
        Long c = consultant("recruiter1", "Two");
        long first = offer("recruiter1", c, "First Client");
        long second = offer("recruiter1", c, "Second Client");

        Map<String, Object> early = placement(first, "2026-09-30");
        expectProblem(postAs("recruiter1", "/api/placements", early), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"));
        for (int term : new int[] { 0, 61 }) {
            Map<String, Object> bad = placement(first, "2026-11-01");
            bad.put("contractTermMonths", term);
            expectProblem(postAs("recruiter1", "/api/placements", bad), 400, "VALIDATION_FAILED")
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("contractTermMonths"));
        }
        Map<String, Object> zeroRate = placement(first, "2026-11-01");
        zeroRate.put("billRate", 0);
        expectProblem(postAs("recruiter1", "/api/placements", zeroRate), 400, "VALIDATION_FAILED");

        postAs("recruiter1", "/api/placements", placement(first, "2026-11-01")).andExpect(status().isCreated());
        expectProblem(postAs("recruiter1", "/api/placements", placement(second, "2026-11-01")), 422, "ALREADY_PLACED");
    }

    @Test
    void fr074_adminEditsAreRecordedAndValidated() throws Exception {
        Long c = consultant("recruiter1", "Edi");
        JsonNode created = body(postAs("recruiter1", "/api/placements",
                placement(offer("recruiter1", c, "Edit Client"), "2026-11-01")).andExpect(status().isCreated()));
        long pid = created.get("id").asLong();
        long version = created.get("version").asLong();

        expectProblem(patchAs("recruiter1", "/api/placements/{id}", map("billRate", 99, "version", version), pid), 403,
                "NOT_AUTHORIZED");
        expectProblem(patchAs("manager", "/api/placements/{id}", map("billRate", 99, "version", version), pid), 403,
                "NOT_AUTHORIZED");
        expectProblem(patchAs("admin", "/api/placements/{id}", map("startDate", "2026-09-30", "version", version), pid),
                400, "VALIDATION_FAILED").andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"));
        getAs("admin", "/api/placements/{id}", pid).andExpect(jsonPath("$.startDate").value("2026-11-01"));

        patchAs("admin", "/api/placements/{id}",
                map("startDate", "2026-11-15", "billRate", 101.25, "contractTermMonths", 6, "version", version), pid)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-11-15"))
                .andExpect(jsonPath("$.expectedEndDate").value("2027-05-15"));
        JsonNode history = body(getAs("admin", "/api/placements/{id}", pid)).get("history");
        java.util.Set<String> fields = new java.util.HashSet<>();
        history.forEach(h -> {
            if ("FIELD_EDIT".equals(h.get("changeType").asText())) {
                fields.add(h.get("field").asText());
                assertThat(h.get("actor").asText()).isEqualTo("Alex Admin");
            }
        });
        assertThat(fields).containsExactlyInAnyOrder("startDate", "billRate", "contractTermMonths");
        expectProblem(patchAs("admin", "/api/placements/{id}", map("contractTermMonths", 61, "version", version + 1),
                pid), 400, "VALIDATION_FAILED");
        expectProblem(patchAs("admin", "/api/placements/{id}", map("billRate", 1, "version", version), pid), 409,
                "CONCURRENT_MODIFICATION");
    }

    @Test
    void creationIsAtomic() throws Exception {
        Long c = consultant("recruiter1", "Ato");
        long sub = offer("recruiter1", c, "Atomic Client");
        // Stub the spy itself: the injected bean is the transactional proxy around it.
        HistoryService spy = AopTestUtils.getUltimateTargetObject(historySpy);
        doThrow(new IllegalStateException("simulated failure after the submission update"))
                .when(spy).recordSystemStatusChange(eq(HistoryEntityType.CONSULTANT), any(), any(), any(), any(),
                        eq("PLACED"), any(), any(), any());
        try {
            postAs("recruiter1", "/api/placements", placement(sub, "2026-11-01"))
                    .andExpect(status().isInternalServerError());
        } finally {
            reset(spy);
        }
        getAs("admin", "/api/submissions/{id}", sub).andExpect(jsonPath("$.status").value("OFFER"));
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.INTERVIEWING);
        getAs("admin", "/api/placements?size=100")
                .andExpect(jsonPath("$.items[?(@.consultant.id == " + c + ")]").doesNotExist());
        postAs("recruiter1", "/api/placements", placement(sub, "2026-11-01")).andExpect(status().isCreated());
    }

    @Test
    void listFilters() throws Exception {
        Long c = consultant("recruiter2", "Lis");
        JsonNode p = body(postAs("recruiter2", "/api/placements", placement(offer("recruiter2", c, "List Client"),
                "2026-12-01")).andExpect(status().isCreated()));
        long clientId = p.get("client").get("id").asLong();
        getAs("admin", "/api/placements?clientId=" + clientId).andExpect(jsonPath("$.totalItems").value(1));
        getAs("admin", "/api/placements?clientId=" + clientId + "&startFrom=2026-12-01&startTo=2026-12-31")
                .andExpect(jsonPath("$.totalItems").value(1));
        getAs("admin", "/api/placements?clientId=" + clientId + "&startFrom=2026-12-02")
                .andExpect(jsonPath("$.totalItems").value(0));
        getAs("admin", "/api/placements?clientId=" + clientId + "&createdFrom=2026-10-01&createdTo=2026-10-31")
                .andExpect(jsonPath("$.totalItems").value(1));
        getAs("admin", "/api/placements?clientId=" + clientId + "&createdFrom=2026-11-01")
                .andExpect(jsonPath("$.totalItems").value(0));
        getAs("admin", "/api/placements?recruiterId=" + data.recruiterIdOfUser("recruiter2") + "&size=100")
                .andExpect(jsonPath("$.items[*].recruiter.fullName").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("Marcus Lee"))));
        getAs("admin", "/api/placements?sort=startDate,desc").andExpect(status().isOk());
    }
}
