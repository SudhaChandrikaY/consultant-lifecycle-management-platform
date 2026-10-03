package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.support.IntegrationTestBase;
import com.ensar.clmp.support.TestDataFactory;

import tools.jackson.databind.JsonNode;

/** US5 — Submit consultants and track progress (AS 5.1–5.9, FR-050–FR-064). */
class US5SubmissionIT extends IntegrationTestBase {

    private Long consultant(String recruiterUser, ConsultantStatus status, String first) {
        return data.consultant(TestDataFactory.completeProfile(first, "Submitted", "Java"), status,
                data.recruiterIdOfUser(recruiterUser));
    }

    private Map<String, Object> body(Long consultantId, String vendor, String client, String job, boolean submitNow) {
        return map("consultantId", consultantId, "vendorName", vendor, "clientName", client, "jobTitle", job,
                "billRate", 85.00, "submitNow", submitNow);
    }

    private JsonNode submit(String user, Long consultantId, String vendor, String client, String job) throws Exception {
        return body(postAs(user, "/api/submissions", body(consultantId, vendor, client, job, true))
                .andExpect(status().isCreated()));
    }

    private JsonNode move(String user, long id, String target, String note) throws Exception {
        long version = body(getAs("admin", "/api/submissions/{id}", id)).get("version").asLong();
        return body(postAs(user, "/api/submissions/{id}/status",
                map("targetStatus", target, "note", note, "version", version), id).andExpect(status().isOk()));
    }

    private void moveAll(String user, long id, String... targets) throws Exception {
        for (String t : targets) {
            move(user, id, t, null);
        }
    }

    private long consultantVersion(Long id) throws Exception {
        return body(getAs("admin", "/api/consultants/{id}", id)).get("version").asLong();
    }

    @Test
    void as5_1_draftOrSubmittedWithToday() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Abe");
        postAs("recruiter1", "/api/submissions", body(c, "Draft Vendor", "Draft Client", "Dev", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.submittedDate").isEmpty())
                .andExpect(jsonPath("$.recruiter.fullName").value("Riya Patel"));
        postAs("recruiter1", "/api/submissions", body(c, "Now Vendor", "Now Client", "Dev", true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedDate").value("2026-10-15"))
                .andExpect(jsonPath("$.billRate").value(85.0))
                .andExpect(jsonPath("$.vendor.name").value("Now Vendor"));
    }

    @Test
    void as5_2_ineligibleConsultantsAreRefused() throws Exception {
        for (ConsultantStatus s : new ConsultantStatus[] { ConsultantStatus.BENCH, ConsultantStatus.HOLD,
                ConsultantStatus.INACTIVE, ConsultantStatus.PLACED, ConsultantStatus.ACTIVE_PROJECT }) {
            Long c = consultant("recruiter1", s, "Ina");
            expectProblem(postAs("recruiter1", "/api/submissions", body(c, "V", "C", "Dev", true)), 422,
                    "CONSULTANT_NOT_ELIGIBLE").andExpect(jsonPath("$.consultantStatus").value(s.name()));
        }
        for (ConsultantStatus s : new ConsultantStatus[] { ConsultantStatus.MARKETING, ConsultantStatus.INTERVIEWING }) {
            postAs("recruiter1", "/api/submissions", body(consultant("recruiter1", s, "Ok"), "V", "C", "Dev", true))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void as5_3_duplicateWarningAndAcknowledgement() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Dup");
        long first = submit("recruiter1", c, "Acme Staffing", "Globex", "Java Developer").get("id").asLong();
        move("recruiter1", first, "WITHDRAWN", null);
        long second = body(postAs("admin", "/api/submissions",
                map("consultantId", c, "vendorName", "ACME STAFFING", "clientName", "globex", "jobTitle",
                        "java developer", "billRate", 80, "submitNow", true, "acknowledgeDuplicate", true))
                .andExpect(status().isCreated())).get("id").asLong();
        move("admin", second, "REJECTED", null);

        expectProblem(postAs("recruiter1", "/api/submissions",
                body(c, " acme  staffing ", "GLOBEX", "  Java   developer", true)), 409, "DUPLICATE_SUBMISSION")
                .andExpect(jsonPath("$.duplicates.length()").value(2))
                .andExpect(jsonPath("$.duplicates[*].id").value(hasItem((int) first)))
                .andExpect(jsonPath("$.duplicates[*].status").value(hasItem("WITHDRAWN")))
                .andExpect(jsonPath("$.duplicates[*].status").value(hasItem("REJECTED")))
                .andExpect(jsonPath("$.duplicates[0].recruiterName").value("Riya Patel"))
                .andExpect(jsonPath("$.duplicates[0].submittedDate").value("2026-10-15"));

        Map<String, Object> confirmed = body(c, " acme  staffing ", "GLOBEX", "  Java   developer", true);
        confirmed.put("acknowledgeDuplicate", true);
        long third = body(postAs("recruiter1", "/api/submissions", confirmed).andExpect(status().isCreated()))
                .get("id").asLong();
        JsonNode detail = body(getAs("recruiter1", "/api/submissions/{id}", third));
        assertThat(detail.get("duplicateAcknowledgement").get("acknowledgedBy").asText()).isEqualTo("Riya Patel");
        assertThat(detail.get("duplicateAcknowledgement").get("acknowledgedAt").asText())
                .isEqualTo("2026-10-15T15:00:00Z");
        Set<Long> earlier = new HashSet<>();
        detail.get("duplicateAcknowledgement").get("earlierSubmissionIds").forEach(n -> earlier.add(n.asLong()));
        assertThat(earlier).containsExactlyInAnyOrder(first, second);
        // Same vendor resolved, not duplicated.
        assertThat(detail.get("vendor").get("name").asText()).isEqualTo("Acme Staffing");
    }

    @Test
    void as5_4_onlyAllowedTransitionsAndTimeline() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Tim");
        long id = submit("recruiter1", c, "Tl Vendor", "Tl Client", "Dev").get("id").asLong();
        JsonNode d = body(getAs("recruiter1", "/api/submissions/{id}", id));
        assertThat(d.get("allowedTransitions").toString())
                .isEqualTo("[\"UNDER_REVIEW\",\"INTERVIEW_SCHEDULED\",\"REJECTED\",\"WITHDRAWN\"]");
        expectProblem(postAs("recruiter1", "/api/submissions/{id}/status",
                map("targetStatus", "OFFER", "version", d.get("version").asLong()), id), 422, "INVALID_TRANSITION")
                .andExpect(jsonPath("$.allowedTransitions").value(hasItem("UNDER_REVIEW")));
        expectProblem(postAs("recruiter1", "/api/submissions/{id}/status",
                map("targetStatus", "PLACED", "version", d.get("version").asLong()), id), 422, "INVALID_TRANSITION");

        move("recruiter1", id, "UNDER_REVIEW", "Vendor confirmed receipt");
        JsonNode timeline = body(getAs("recruiter1", "/api/submissions/{id}", id)).get("timeline");
        assertThat(timeline.get(0).get("oldValue").asText()).isEqualTo("SUBMITTED");
        assertThat(timeline.get(0).get("newValue").asText()).isEqualTo("UNDER_REVIEW");
        assertThat(timeline.get(0).get("note").asText()).isEqualTo("Vendor confirmed receipt");
        assertThat(timeline.get(0).get("actor").asText()).isEqualTo("Riya Patel");
        assertThat(timeline.get(0).get("occurredAt").asText()).isEqualTo("2026-10-15T15:00:00Z");

        move("recruiter1", id, "REJECTED", null);
        assertThat(body(getAs("recruiter1", "/api/submissions/{id}", id)).get("allowedTransitions")).isEmpty();

        // DRAFT -> SUBMITTED sets the submitted date (default today; never in the future).
        long draft = body(postAs("recruiter1", "/api/submissions", body(c, "D2", "C2", "Dev", false))
                .andExpect(status().isCreated())).get("id").asLong();
        expectProblem(postAs("recruiter1", "/api/submissions/{id}/status",
                map("targetStatus", "SUBMITTED", "submittedDate", "2026-10-16", "version", 0), draft), 400,
                "VALIDATION_FAILED").andExpect(jsonPath("$.fieldErrors[0].field").value("submittedDate"));
        postAs("recruiter1", "/api/submissions/{id}/status",
                map("targetStatus", "SUBMITTED", "submittedDate", "2026-10-10", "version", 0), draft)
                .andExpect(status().isOk()).andExpect(jsonPath("$.submittedDate").value("2026-10-10"));
    }

    @Test
    void as5_5_interviewScheduledMakesConsultantInterviewing() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Ivan");
        long id = submit("recruiter1", c, "Acme Interviews", "Initech", "Java Developer").get("id").asLong();
        move("recruiter1", id, "INTERVIEW_SCHEDULED", null);
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.INTERVIEWING);
        getAs("admin", "/api/consultants/{id}/history", c)
                .andExpect(jsonPath("$.items[0].systemTriggered").value(true))
                .andExpect(jsonPath("$.items[0].trigger.event").value("SUBMISSION_INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.items[0].trigger.entityType").value("SUBMISSION"))
                .andExpect(jsonPath("$.items[0].trigger.entityId").value(id))
                .andExpect(jsonPath("$.items[0].actor").value("Riya Patel"))
                .andExpect(jsonPath("$.items[0].oldValue").value("READY"))
                .andExpect(jsonPath("$.items[0].description")
                        .value("Submission for Initech / Java Developer moved to Interview Scheduled"));

        // A second submission entering interviews leaves an Interviewing consultant unchanged.
        long other = submit("recruiter1", c, "Other V", "Other C", "Dev").get("id").asLong();
        move("recruiter1", other, "INTERVIEW_SCHEDULED", null);
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.INTERVIEWING);
        move("recruiter1", id, "REJECTED", null);
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.INTERVIEWING);
    }

    @Test
    void as5_6_leavingInterviewStagesReturnsToMarketingOrReady() throws Exception {
        // With an Active marketing assignment -> back to Marketing.
        Long withMarketing = consultant("recruiter1", ConsultantStatus.READY, "Mia");
        long ma = body(postAs("recruiter1", "/api/marketing-assignments",
                map("consultantId", withMarketing, "startDate", "2026-10-01", "targetDate", "2026-11-01"))
                .andExpect(status().isCreated())).get("id").asLong();
        postAs("recruiter1", "/api/marketing-assignments/{id}/transition", map("targetStatus", "ACTIVE", "version", 0), ma)
                .andExpect(status().isOk());
        long s1 = submit("recruiter1", withMarketing, "Rv", "Rc", "Dev").get("id").asLong();
        moveAll("recruiter1", s1, "INTERVIEW_SCHEDULED", "INTERVIEW_CLEARED", "OFFER");
        assertThat(data.consultantStatus(withMarketing)).isEqualTo(ConsultantStatus.INTERVIEWING);
        move("recruiter1", s1, "REJECTED", "Offer declined");
        assertThat(data.consultantStatus(withMarketing)).isEqualTo(ConsultantStatus.MARKETING);
        getAs("admin", "/api/consultants/{id}/history", withMarketing)
                .andExpect(jsonPath("$.items[0].trigger.event").value("SUBMISSION_LEFT_INTERVIEW_STAGES"));

        // Without one -> back to Ready.
        Long noMarketing = consultant("recruiter1", ConsultantStatus.READY, "Rex");
        long s2 = submit("recruiter1", noMarketing, "Rv2", "Rc2", "Dev").get("id").asLong();
        move("recruiter1", s2, "INTERVIEW_SCHEDULED", null);
        move("recruiter1", s2, "WITHDRAWN", null);
        assertThat(data.consultantStatus(noMarketing)).isEqualTo(ConsultantStatus.READY);
    }

    @Test
    void as5_7_notesAreAppendOnly() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Nol");
        long id = body(postAs("recruiter1", "/api/submissions",
                map("consultantId", c, "vendorName", "Nv", "clientName", "Nc", "jobTitle", "Dev", "billRate", 70,
                        "submitNow", true, "note", "Strong Spring match")).andExpect(status().isCreated()))
                .get("id").asLong();
        postAs("recruiter1", "/api/submissions/{id}/notes", map("body", "Vendor asked for references"), id)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.author").value("Riya Patel"));
        getAs("recruiter1", "/api/submissions/{id}", id)
                .andExpect(jsonPath("$.notes.length()").value(2))
                .andExpect(jsonPath("$.notes[0].body").value("Strong Spring match"))
                .andExpect(jsonPath("$.notes[1].createdAt").value("2026-10-15T15:00:00Z"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/submissions/{id}/notes/1", id).session(loginAs("recruiter1"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
    }

    @Test
    void as5_8_filters() throws Exception {
        Long c = consultant("recruiter2", ConsultantStatus.READY, "Fil");
        JsonNode a = submit("recruiter2", c, "Filter Vendor A", "Filter Client A", "Dev");
        JsonNode b = submit("recruiter2", c, "Filter Vendor B", "Filter Client B", "Dev");
        move("recruiter2", b.get("id").asLong(), "UNDER_REVIEW", null);
        long vendorA = a.get("vendor").get("id").asLong();
        long clientB = b.get("client").get("id").asLong();
        Long marcus = data.recruiterIdOfUser("recruiter2");

        getAs("admin", "/api/submissions?vendorId=" + vendorA).andExpect(jsonPath("$.totalItems").value(1));
        getAs("admin", "/api/submissions?clientId=" + clientB + "&status=UNDER_REVIEW")
                .andExpect(jsonPath("$.totalItems").value(1));
        getAs("admin", "/api/submissions?clientId=" + clientB + "&status=SUBMITTED")
                .andExpect(jsonPath("$.totalItems").value(0));
        getAs("admin", "/api/submissions?consultantId=" + c + "&recruiterId=" + marcus)
                .andExpect(jsonPath("$.totalItems").value(2));
        getAs("admin", "/api/submissions?consultantId=" + c + "&submittedFrom=2026-10-15&submittedTo=2026-10-15")
                .andExpect(jsonPath("$.totalItems").value(2));
        getAs("admin", "/api/submissions?consultantId=" + c + "&submittedFrom=2026-10-16")
                .andExpect(jsonPath("$.totalItems").value(0));
        getAs("admin", "/api/submissions?sort=billRate,desc").andExpect(status().isOk());
        getAs("recruiter2", "/api/vendors?q=filter vendor").andExpect(jsonPath("$.length()").value(2));
        getAs("recruiter2", "/api/clients?q=FILTER CLIENT A").andExpect(jsonPath("$[0].name").value("Filter Client A"));
        expectProblem(getAs("hr", "/api/vendors"), 403, "NOT_AUTHORIZED");
    }

    @Test
    void as5_9_managerViewsButCannotAct() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Man");
        long id = submit("recruiter1", c, "Mv", "Mc", "Dev").get("id").asLong();
        getAs("manager", "/api/submissions/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedTransitions").isEmpty())
                .andExpect(jsonPath("$.canCreatePlacement").value(false))
                .andExpect(jsonPath("$.timeline").isArray());
        expectProblem(postAs("manager", "/api/submissions", body(c, "V", "C", "J", true)), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("manager", "/api/submissions/{id}/status", map("targetStatus", "UNDER_REVIEW", "version", 0),
                id), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("manager", "/api/submissions/{id}/notes", map("body", "x"), id), 403, "NOT_AUTHORIZED");
    }

    @Test
    void fr063_and_fr070_ownershipAfterReassignment() throws Exception {
        Long c = consultant("recruiter2", ConsultantStatus.READY, "Own");
        long id = submit("recruiter2", c, "Ov", "Oc", "Dev").get("id").asLong();
        expectProblem(getAs("recruiter1", "/api/submissions/{id}", id), 403, "NOT_AUTHORIZED");

        postAs("manager", "/api/consultants/{id}/recruiter",
                map("recruiterId", data.recruiterIdOfUser("recruiter1"), "version", consultantVersion(c)), c)
                .andExpect(status().isOk());
        getAs("recruiter1", "/api/submissions/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.recruiter.fullName").value("Marcus Lee"));
        moveAll("recruiter1", id, "INTERVIEW_SCHEDULED", "INTERVIEW_CLEARED", "OFFER");
        getAs("recruiter2", "/api/submissions/{id}", id).andExpect(status().isOk());
        getAs("recruiter2", "/api/submissions?size=100")
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").exists());

        getAs("recruiter1", "/api/submissions/{id}", id).andExpect(jsonPath("$.canCreatePlacement").value(false));
        getAs("recruiter2", "/api/submissions/{id}", id).andExpect(jsonPath("$.canCreatePlacement").value(true));
        getAs("admin", "/api/submissions/{id}", id).andExpect(jsonPath("$.canCreatePlacement").value(true));
    }

    @Test
    void noGeneralEditEndpoint() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Put");
        long id = submit("recruiter1", c, "Pv", "Pc", "Dev").get("id").asLong();
        putAs("admin", "/api/submissions/{id}", map("jobTitle", "Changed"), id).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void validationOfRateDateAndCounterparty() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Val");
        for (Object rate : new Object[] { 0, -5 }) {
            Map<String, Object> b = body(c, "V", "C", "Dev", true);
            b.put("billRate", rate);
            expectProblem(postAs("recruiter1", "/api/submissions", b), 400, "VALIDATION_FAILED")
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("billRate"));
        }
        Map<String, Object> future = body(c, "V", "C", "Dev", true);
        future.put("submittedDate", "2026-10-16");
        expectProblem(postAs("recruiter1", "/api/submissions", future), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("submittedDate"));
        Map<String, Object> noVendor = body(c, null, "C", "Dev", true);
        expectProblem(postAs("recruiter1", "/api/submissions", noVendor), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("vendorName"));
    }

    @Test
    void fr051_vendorFindOrCreateIsNormalized() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Ven");
        long v1 = submit("recruiter1", c, "Normal  Vendor", "Norm C1", "Dev").get("vendor").get("id").asLong();
        long v2 = submit("recruiter1", c, "  normal vendor ", "Norm C2", "Dev").get("vendor").get("id").asLong();
        assertThat(v1).isEqualTo(v2);
        Map<String, Object> byId = map("consultantId", c, "vendorId", v1, "clientName", "Norm C3", "jobTitle", "Dev",
                "billRate", 50, "submitNow", true);
        postAs("recruiter1", "/api/submissions", byId).andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendor.name").value("Normal  Vendor"));
    }

    @Test
    void inactiveRefusedWithOpenSubmissionsAndHoldBlocksNewOnes() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Hol");
        long open = submit("recruiter1", c, "Hv", "Hc", "Dev").get("id").asLong();
        expectProblem(postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "INACTIVE", "reason", "Leaving", "version", consultantVersion(c)), c), 422,
                "OPEN_SUBMISSIONS_EXIST").andExpect(jsonPath("$.openSubmissionIds[0]").value(open));

        postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "HOLD", "reason", "Leave", "version", consultantVersion(c)), c)
                .andExpect(status().isOk());
        move("recruiter1", open, "INTERVIEW_SCHEDULED", null);
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.HOLD);
        expectProblem(postAs("recruiter1", "/api/submissions", body(c, "Hv2", "Hc2", "Dev", true)), 422,
                "CONSULTANT_NOT_ELIGIBLE");
        getAs("recruiter1", "/api/consultants/{id}", c)
                .andExpect(jsonPath("$.openSubmissionsWhileOnHold[0].id").value(open))
                .andExpect(jsonPath("$.submissions[0].vendorName").value("Hv"));

        move("recruiter1", open, "WITHDRAWN", null);
        postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "INACTIVE", "reason", "Leaving", "version", consultantVersion(c)), c)
                .andExpect(status().isOk());
    }

    @Test
    void fr064_hrSeesNoCommercialDataAndGenericHistory() throws Exception {
        Long c = consultant("recruiter1", ConsultantStatus.READY, "Hrv");
        long id = submit("recruiter1", c, "Secret Vendor", "Secret Client", "Secret Job").get("id").asLong();
        move("recruiter1", id, "INTERVIEW_SCHEDULED", null);

        String detail = getAs("hr", "/api/consultants/{id}", c).andExpect(status().isOk())
                .andExpect(jsonPath("$.submissions").doesNotExist())
                .andExpect(jsonPath("$.openSubmissionsWhileOnHold").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).doesNotContain("Secret");
        String history = getAs("hr", "/api/consultants/{id}/history", c)
                .andExpect(jsonPath("$.items[0].description").value("Submission moved to Interview Scheduled"))
                .andExpect(jsonPath("$.items[0].trigger.entityType").isEmpty())
                .andExpect(jsonPath("$.items[0].trigger.entityId").isEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(history).doesNotContain("Secret");
        expectProblem(getAs("hr", "/api/submissions/{id}", id), 403, "NOT_AUTHORIZED");
    }
}
