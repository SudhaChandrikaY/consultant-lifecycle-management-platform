package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.support.IntegrationTestBase;
import com.ensar.clmp.support.TestDataFactory;

import tools.jackson.databind.JsonNode;

/** US2 — Maintain consultant profiles and readiness (AS 2.1–2.6, FR-020–FR-035, FR-103). */
class US2ConsultantProfileIT extends IntegrationTestBase {

    private Map<String, Object> minimal(String email) {
        return map("firstName", "Nina", "lastName", "Bench", "email", email);
    }

    private Map<String, Object> complete(String email, Long version) {
        return map("firstName", "Carl", "lastName", "Complete", "email", email, "phone", "555-0123", "city", "Edison",
                "state", "NJ", "primarySkill", "Java", "additionalSkills", "Spring", "yearsExperience", 7,
                "visaType", "H1B", "visaExpirationDate", "2027-01-31", "notes", "Private note", "version", version);
    }

    private long createAsHr(Map<String, Object> body) throws Exception {
        JsonNode created = body(postAs("hr", "/api/consultants", body).andExpect(status().isCreated()));
        return created.get("id").asLong();
    }

    @Test
    void as2_1_hrCreatesMinimalConsultantInBench() throws Exception {
        postAs("hr", "/api/consultants", minimal(TestDataFactory.uniqueEmail("nina")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BENCH"))
                .andExpect(jsonPath("$.firstName").value("Nina"))
                .andExpect(jsonPath("$.version").isNumber());
    }

    @Test
    void as2_2_markReadyWithMissingItemsIsRefused() throws Exception {
        long id = createAsHr(minimal(TestDataFactory.uniqueEmail("miss")));
        expectProblem(postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "READY", "version", 0), id), 422, "READINESS_INCOMPLETE")
                .andExpect(jsonPath("$.missingItems[0]").value("phone"))
                .andExpect(jsonPath("$.missingItems").value(hasItem("assignedActiveRecruiter")))
                .andExpect(jsonPath("$.missingItems").value(hasItem("visaType")));
        assertThat(data.consultantStatus(id)).isEqualTo(ConsultantStatus.BENCH);
    }

    @Test
    void as2_3_completeAssignedConsultantBecomesReadyWithHistory() throws Exception {
        long id = createAsHr(complete(TestDataFactory.uniqueEmail("carl"), null));
        data.assignRecruiter(id, data.recruiterIdOfUser("recruiter1"));
        long version = data.consultantVersion(id);

        postAs("hr", "/api/consultants/{id}/status", map("targetStatus", "READY", "version", version), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        getAs("hr", "/api/consultants/{id}/history", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].changeType").value("STATUS"))
                .andExpect(jsonPath("$.items[0].oldValue").value("BENCH"))
                .andExpect(jsonPath("$.items[0].newValue").value("READY"))
                .andExpect(jsonPath("$.items[0].actor").value("Harper HR"))
                .andExpect(jsonPath("$.items[0].occurredAt").value("2026-10-15T15:00:00Z"))
                .andExpect(jsonPath("$.items[0].description").value("Status changed from Bench to Ready"));
    }

    @Test
    void as2_4_searchAndFilters() throws Exception {
        Long riya = data.recruiterIdOfUser("recruiter1");
        Long marcus = data.recruiterIdOfUser("recruiter2");
        data.consultant(TestDataFactory.completeProfile("Quentin", "Filterly", "Haskell"), ConsultantStatus.READY, riya);
        data.consultant(TestDataFactory.completeProfile("Quinn", "Filterly", "Haskell"), ConsultantStatus.HOLD, marcus);
        data.consultant(TestDataFactory.completeProfile("Quade", "Filterly", "Erlang"), ConsultantStatus.BENCH, riya);

        getAs("admin", "/api/consultants?q=filterly").andExpect(jsonPath("$.totalItems").value(3));
        getAs("admin", "/api/consultants?q=QUINN filterly").andExpect(jsonPath("$.totalItems").value(1));
        getAs("admin", "/api/consultants?q=filterly&status=READY&status=HOLD")
                .andExpect(jsonPath("$.totalItems").value(2));
        getAs("admin", "/api/consultants?q=filterly&primarySkill=Haskell")
                .andExpect(jsonPath("$.totalItems").value(2));
        getAs("admin", "/api/consultants?q=filterly&visaType=GREEN_CARD").andExpect(jsonPath("$.totalItems").value(3));
        getAs("admin", "/api/consultants?q=filterly&visaType=H1B").andExpect(jsonPath("$.totalItems").value(0));
        getAs("admin", "/api/consultants?q=filterly&recruiterId=" + marcus)
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("Quinn Filterly"));
        getAs("admin", "/api/consultants?q=filterly&sort=lastName,asc").andExpect(status().isOk());
        expectProblem(getAs("admin", "/api/consultants?sort=email,asc"), 400, "VALIDATION_FAILED");
        getAs("admin", "/api/consultants/skills").andExpect(jsonPath("$").value(hasItem("Haskell")));
    }

    @Test
    void as2_5_duplicateEmailIsRefusedOnCreateAndUpdate() throws Exception {
        String email = TestDataFactory.uniqueEmail("dupe");
        createAsHr(minimal(email));
        expectProblem(postAs("hr", "/api/consultants", minimal(email.toUpperCase())), 409, "DUPLICATE_EMAIL")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));

        long other = createAsHr(minimal(TestDataFactory.uniqueEmail("other")));
        Map<String, Object> update = minimal(email);
        update.put("version", 0);
        expectProblem(putAs("hr", "/api/consultants/{id}", update, other), 409, "DUPLICATE_EMAIL");
    }

    @Test
    void as2_6_holdWithReasonIsRecorded() throws Exception {
        Long id = data.consultant(TestDataFactory.completeProfile("Holly", "Hold", "Go"), ConsultantStatus.READY,
                data.recruiterIdOfUser("recruiter1"));
        long version = data.consultantVersion(id);
        expectProblem(postAs("hr", "/api/consultants/{id}/status", map("targetStatus", "HOLD", "version", version), id),
                400, "VALIDATION_FAILED").andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));

        postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", "HOLD", "reason", "Family emergency", "version", version), id)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HOLD"));
        getAs("admin", "/api/consultants/{id}/history", id)
                .andExpect(jsonPath("$.items[0].reason").value("Family emergency"))
                .andExpect(jsonPath("$.items[0].actor").value("Harper HR"))
                .andExpect(jsonPath("$.items[0].newValue").value("HOLD"));
    }

    @Test
    void fr024_listItemsNeverContainContactFields() throws Exception {
        String body = getAs("admin", "/api/consultants?size=100").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("\"email\"").doesNotContain("\"phone\"")
                .doesNotContain("\"visaExpirationDate\"").doesNotContain("\"notes\"").doesNotContain("@");
    }

    @Test
    void fr026_recruiterSeesOnlyOwnConsultants() throws Exception {
        Long riya = data.recruiterIdOfUser("recruiter1");
        Long marcus = data.recruiterIdOfUser("recruiter2");
        Long own = data.consultant(TestDataFactory.completeProfile("Owen", "Owned", "Java"), ConsultantStatus.BENCH, riya);
        Long notOwn = data.consultant(TestDataFactory.completeProfile("Nate", "Other", "Java"), ConsultantStatus.BENCH,
                marcus);

        JsonNode page = body(getAs("recruiter1", "/api/consultants?size=100").andExpect(status().isOk()));
        assertThat(page.get("totalItems").asInt()).isPositive();
        page.get("items").forEach(item -> assertThat(item.get("assignedRecruiter").get("id").asLong()).isEqualTo(riya));

        getAs("recruiter1", "/api/consultants/{id}", own).andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.email").isNotEmpty())
                .andExpect(jsonPath("$.allowedStatusTransitions").isEmpty());
        expectProblem(getAs("recruiter1", "/api/consultants/{id}", notOwn), 403, "NOT_AUTHORIZED");
        expectProblem(getAs("recruiter1", "/api/consultants/{id}/history", notOwn), 403, "NOT_AUTHORIZED");
        expectProblem(putAs("recruiter1", "/api/consultants/{id}", complete(TestDataFactory.uniqueEmail("x"), 0L), own),
                403, "NOT_AUTHORIZED");
        expectProblem(postAs("recruiter1", "/api/consultants/{id}/status", map("targetStatus", "HOLD", "reason", "x",
                "version", 0), own), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("manager", "/api/consultants", minimal(TestDataFactory.uniqueEmail("m"))), 403,
                "NOT_AUTHORIZED");
    }

    @Test
    void unlinkedRecruiterGetsAnEmptyPage() throws Exception {
        getAs("recruiter3", "/api/consultants").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void fr103_contactVisibleToPermittedViewers() throws Exception {
        Long id = data.consultant(TestDataFactory.completeProfile("Cora", "Contact", "Java"), ConsultantStatus.BENCH,
                data.recruiterIdOfUser("recruiter1"));
        for (String user : new String[] { "admin", "manager", "hr", "recruiter1" }) {
            getAs(user, "/api/consultants/{id}", id).andExpect(status().isOk())
                    .andExpect(jsonPath("$.contact.email").value(org.hamcrest.Matchers.startsWith("cora.")))
                    .andExpect(jsonPath("$.contact.phone").value("555-0100"));
        }
        getAs("manager", "/api/consultants/{id}", id).andExpect(jsonPath("$.allowedStatusTransitions").isEmpty());
        getAs("hr", "/api/consultants/{id}", id)
                .andExpect(jsonPath("$.allowedStatusTransitions").value(hasItem("READY")));
    }

    @Test
    void validationErrorsAreFieldLevel() throws Exception {
        Map<String, Object> body = minimal(TestDataFactory.uniqueEmail("v"));
        body.put("yearsExperience", 51);
        expectProblem(postAs("hr", "/api/consultants", body), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("yearsExperience"));

        Map<String, Object> missing = map("lastName", "Only", "email", "not-an-email");
        expectProblem(postAs("hr", "/api/consultants", missing), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("firstName")))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("email")));
    }

    @Test
    void staleVersionOnPutIsAConflict() throws Exception {
        long id = createAsHr(minimal(TestDataFactory.uniqueEmail("stale")));
        String email = TestDataFactory.uniqueEmail("stale2");
        putAs("hr", "/api/consultants/{id}", complete(email, 0L), id).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        expectProblem(putAs("hr", "/api/consultants/{id}", complete(email, 0L), id), 409, "CONCURRENT_MODIFICATION")
                .andExpect(jsonPath("$.currentVersion").value(1));
    }

    @Test
    void profileEditRecordsFieldNamesOnly() throws Exception {
        long id = createAsHr(minimal(TestDataFactory.uniqueEmail("prof")));
        Map<String, Object> body = minimal(TestDataFactory.uniqueEmail("prof2"));
        body.put("phone", "555-9876");
        body.put("version", 0);
        putAs("hr", "/api/consultants/{id}", body, id).andExpect(status().isOk());

        String history = getAs("admin", "/api/consultants/{id}/history", id)
                .andExpect(jsonPath("$.items[0].changeType").value("PROFILE_UPDATED"))
                .andExpect(jsonPath("$.items[0].field").value("email, phone"))
                .andExpect(jsonPath("$.items[0].oldValue").isEmpty())
                .andExpect(jsonPath("$.items[0].newValue").isEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(history).doesNotContain("555-9876").doesNotContain("prof2");
    }

    @Test
    void pageSizeAboveLimitIsRejected() throws Exception {
        expectProblem(getAs("admin", "/api/consultants?size=101"), 400, "VALIDATION_FAILED");
        getAs("admin", "/api/consultants?size=100").andExpect(status().isOk());
    }

    @Test
    void unknownConsultantIs404() throws Exception {
        expectProblem(getAs("admin", "/api/consultants/{id}", 999999), 404, "NOT_FOUND");
    }

    @Test
    void referenceDataIsAvailableToAllRoles() throws Exception {
        for (String user : new String[] { "admin", "manager", "recruiter1", "hr" }) {
            getAs(user, "/api/reference").andExpect(status().isOk())
                    .andExpect(jsonPath("$.teams.length()").value(4))
                    .andExpect(jsonPath("$.regions.length()").value(4))
                    .andExpect(jsonPath("$.visaTypes").value(hasItem("H4_EAD")))
                    .andExpect(jsonPath("$.consultantStatuses").value(hasItem("ACTIVE_PROJECT")))
                    .andExpect(jsonPath("$.consultantStatuses").value(not(hasItem("UNKNOWN"))));
        }
    }
}
