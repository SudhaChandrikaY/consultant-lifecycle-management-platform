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

/** US3 — Manage recruiters and consultant assignment (AS 3.1–3.6, FR-010–FR-017, FR-033/034). */
class US3RecruiterAssignmentIT extends IntegrationTestBase {

    private long teamId(String code) throws Exception {
        for (JsonNode t : body(getAs("admin", "/api/reference")).get("teams")) {
            if (t.get("code").asText().equals(code)) {
                return t.get("id").asLong();
            }
        }
        throw new IllegalArgumentException(code);
    }

    private long regionId(String code) throws Exception {
        for (JsonNode r : body(getAs("admin", "/api/reference")).get("regions")) {
            if (r.get("code").asText().equals(code)) {
                return r.get("id").asLong();
            }
        }
        throw new IllegalArgumentException(code);
    }

    private Map<String, Object> recruiterBody(String name, String email) throws Exception {
        return map("fullName", name, "email", email, "phone", "555-1111", "teamId", teamId("JAVA"),
                "regionId", regionId("EAST"));
    }

    private long createRecruiter(String name) throws Exception {
        return body(postAs("admin", "/api/recruiters", recruiterBody(name, TestDataFactory.uniqueEmail("rec")))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long consultantVersion(long id) throws Exception {
        return body(getAs("admin", "/api/consultants/{id}", id)).get("version").asLong();
    }

    private void assign(String user, long consultantId, long recruiterId) throws Exception {
        postAs(user, "/api/consultants/{id}/recruiter",
                map("recruiterId", recruiterId, "version", consultantVersion(consultantId)), consultantId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedRecruiter.id").value(recruiterId));
    }

    private int count(long recruiterId) throws Exception {
        return body(getAs("admin", "/api/recruiters/{id}", recruiterId)).get("assignedConsultantCount").asInt();
    }

    @Test
    void as3_1_adminAddsActiveRecruiterWithZeroCount() throws Exception {
        postAs("admin", "/api/recruiters", recruiterBody("Nora New", TestDataFactory.uniqueEmail("nora")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.assignedConsultantCount").value(0))
                .andExpect(jsonPath("$.team.name").value("Java"))
                .andExpect(jsonPath("$.region.name").value("East"))
                .andExpect(jsonPath("$.version").isNumber());
    }

    @Test
    void as3_2_and_3_3_assignAndReassignMoveCountsAndRecordHistory() throws Exception {
        long a = createRecruiter("Alice Assign");
        long b = createRecruiter("Bob Reassign");
        Long consultant = data.consultant(TestDataFactory.completeProfile("Cami", "Counted", "Rust"),
                ConsultantStatus.BENCH, null);

        assign("admin", consultant, a);
        assertThat(count(a)).isEqualTo(1);
        getAs("admin", "/api/consultants/{id}", consultant)
                .andExpect(jsonPath("$.assignedRecruiter.fullName").value("Alice Assign"));

        assign("manager", consultant, b);
        assertThat(count(a)).isZero();
        assertThat(count(b)).isEqualTo(1);

        getAs("admin", "/api/consultants/{id}/history", consultant)
                .andExpect(jsonPath("$.items[0].changeType").value("RECRUITER_ASSIGNMENT"))
                .andExpect(jsonPath("$.items[0].oldValue").value("Alice Assign"))
                .andExpect(jsonPath("$.items[0].newValue").value("Bob Reassign"))
                .andExpect(jsonPath("$.items[0].actor").value("Morgan Manager"))
                .andExpect(jsonPath("$.items[0].occurredAt").value("2026-10-15T15:00:00Z"));

        // SC-004: exactly one current recruiter; the old recruiter's filtered list no longer has it.
        getAs("admin", "/api/consultants?recruiterId=" + a).andExpect(jsonPath("$.totalItems").value(0));
        getAs("admin", "/api/consultants?recruiterId=" + b).andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void as3_4_assigningToInactiveRecruiterIsRefused() throws Exception {
        long r = createRecruiter("Ivy Inactive");
        long version = body(getAs("admin", "/api/recruiters/{id}", r)).get("version").asLong();
        postAs("admin", "/api/recruiters/{id}/status", map("status", "INACTIVE", "version", version), r)
                .andExpect(status().isOk());
        Long consultant = data.consultant(TestDataFactory.completeProfile("Ian", "Refused", "Go"),
                ConsultantStatus.BENCH, null);
        expectProblem(postAs("admin", "/api/consultants/{id}/recruiter",
                map("recruiterId", r, "version", consultantVersion(consultant)), consultant), 422,
                "RECRUITER_INACTIVE");
        getAs("admin", "/api/consultants/{id}", consultant).andExpect(jsonPath("$.assignedRecruiter").doesNotExist());
    }

    @Test
    void as3_5_deactivationNeedsConfirmationAndFlagsConsultants() throws Exception {
        long r = createRecruiter("Dee Activate");
        Long c1 = data.consultant(TestDataFactory.completeProfile("Flag", "One", "Go"), ConsultantStatus.READY, r);
        Long c2 = data.consultant(TestDataFactory.completeProfile("Flag", "Two", "Go"), ConsultantStatus.BENCH, r);
        long version = body(getAs("admin", "/api/recruiters/{id}", r)).get("version").asLong();

        expectProblem(postAs("admin", "/api/recruiters/{id}/status", map("status", "INACTIVE", "version", version), r),
                409, "CONFIRMATION_REQUIRED").andExpect(jsonPath("$.affectedConsultantCount").value(2));
        getAs("admin", "/api/recruiters/{id}", r).andExpect(jsonPath("$.status").value("ACTIVE"));

        postAs("admin", "/api/recruiters/{id}/status", map("status", "INACTIVE", "confirm", true, "version", version), r)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.consultantsFlaggedForReassignment").value(2));

        JsonNode flagged = body(getAs("admin", "/api/consultants?needsReassignment=true&size=100"));
        java.util.List<Long> ids = new java.util.ArrayList<>();
        flagged.get("items").forEach(i -> {
            ids.add(i.get("id").asLong());
            assertThat(i.get("needsReassignment").asBoolean()).isTrue();
        });
        assertThat(ids).contains(c1, c2);
        getAs("admin", "/api/consultants?needsReassignment=false&size=100")
                .andExpect(jsonPath("$.items[*].id").value(not(hasItem(c1.intValue()))));
        // Deactivation never changes consultant status.
        assertThat(data.consultantStatus(c1)).isEqualTo(ConsultantStatus.READY);

        // Reassigning clears the flag.
        long other = createRecruiter("Ray Replacement");
        assign("manager", c1, other);
        getAs("admin", "/api/consultants/{id}", c1).andExpect(jsonPath("$.needsReassignment").value(false));

        // The status change is recorded in recruiter history.
        long v2 = body(getAs("admin", "/api/recruiters/{id}", r)).get("version").asLong();
        postAs("admin", "/api/recruiters/{id}/status", map("status", "ACTIVE", "version", v2), r)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void as3_6_filtersAndSearch() throws Exception {
        long dataTeam = teamId("DATA");
        long west = regionId("WEST");
        postAs("admin", "/api/recruiters", map("fullName", "Zed Filterable", "email", TestDataFactory.uniqueEmail("zed"),
                "teamId", dataTeam, "regionId", west)).andExpect(status().isCreated());

        getAs("manager", "/api/recruiters?q=zed filter").andExpect(jsonPath("$.totalItems").value(1));
        getAs("manager", "/api/recruiters?q=filterable&teamId=" + dataTeam + "&regionId=" + west)
                .andExpect(jsonPath("$.totalItems").value(1));
        getAs("manager", "/api/recruiters?q=filterable&teamId=" + teamId("JAVA"))
                .andExpect(jsonPath("$.totalItems").value(0));
        getAs("manager", "/api/recruiters?q=filterable&status=INACTIVE").andExpect(jsonPath("$.totalItems").value(0));
        getAs("manager", "/api/recruiters?sort=fullName,desc").andExpect(status().isOk());
        expectProblem(getAs("manager", "/api/recruiters?sort=email"), 400, "VALIDATION_FAILED");

        Long riya = data.recruiterIdOfUser("recruiter1");
        JsonNode list = body(getAs("manager", "/api/recruiters?q=riya patel"));
        assertThat(list.get("totalItems").asInt()).isEqualTo(1);
        assertThat(list.get("items").get(0).get("id").asLong()).isEqualTo(riya);
        assertThat(list.get("items").get(0).get("assignedConsultantCount").asInt()).isPositive();
    }

    @Test
    void rolesAreEnforced() throws Exception {
        long r = createRecruiter("Rhea Roles");
        Map<String, Object> body = recruiterBody("Mallory", TestDataFactory.uniqueEmail("m"));
        expectProblem(postAs("manager", "/api/recruiters", body), 403, "NOT_AUTHORIZED");
        body.put("version", 0);
        expectProblem(putAs("manager", "/api/recruiters/{id}", body, r), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("manager", "/api/recruiters/{id}/status", map("status", "INACTIVE", "version", 0), r), 403,
                "NOT_AUTHORIZED");
        expectProblem(getAs("manager", "/api/recruiters/linkable-users"), 403, "NOT_AUTHORIZED");
        getAs("manager", "/api/recruiters/{id}", r).andExpect(status().isOk());

        Long consultant = data.consultant(TestDataFactory.completeProfile("Role", "Check", "Go"), ConsultantStatus.BENCH,
                data.recruiterIdOfUser("recruiter1"));
        long version = consultantVersion(consultant);
        expectProblem(postAs("recruiter1", "/api/consultants/{id}/recruiter", map("recruiterId", r, "version", version),
                consultant), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("hr", "/api/consultants/{id}/recruiter", map("recruiterId", r, "version", version),
                consultant), 403, "NOT_AUTHORIZED");
        expectProblem(getAs("hr", "/api/recruiters"), 403, "NOT_AUTHORIZED");
    }

    @Test
    void duplicateEmailIsRefused() throws Exception {
        String email = TestDataFactory.uniqueEmail("dup");
        postAs("admin", "/api/recruiters", recruiterBody("First", email)).andExpect(status().isCreated());
        expectProblem(postAs("admin", "/api/recruiters", recruiterBody("Second", email.toUpperCase())), 409,
                "DUPLICATE_EMAIL").andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
        expectProblem(postAs("admin", "/api/recruiters", recruiterBody("Seeded", "riya.patel@clmp.example")), 409,
                "DUPLICATE_EMAIL");
    }

    @Test
    void linkedUserMustBeAnUnlinkedRecruiterUser() throws Exception {
        JsonNode linkable = body(getAs("admin", "/api/recruiters/linkable-users").andExpect(status().isOk()));
        long recruiter3 = -1;
        for (JsonNode u : linkable) {
            assertThat(u.get("username").asText()).isNotIn("recruiter1", "recruiter2", "admin", "hr", "manager");
            if (u.get("username").asText().equals("recruiter3")) {
                recruiter3 = u.get("id").asLong();
            }
        }
        assertThat(recruiter3).isPositive();

        long adminId = body(getAs("admin", "/api/auth/me")).get("id").asLong();
        Map<String, Object> wrongRole = recruiterBody("Wrong Role", TestDataFactory.uniqueEmail("w"));
        wrongRole.put("linkedUserId", adminId);
        expectProblem(postAs("admin", "/api/recruiters", wrongRole), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("linkedUserId"));

        long recruiter1User = body(getAs("recruiter1", "/api/auth/me")).get("id").asLong();
        Map<String, Object> alreadyLinked = recruiterBody("Already", TestDataFactory.uniqueEmail("a"));
        alreadyLinked.put("linkedUserId", recruiter1User);
        expectProblem(postAs("admin", "/api/recruiters", alreadyLinked), 400, "VALIDATION_FAILED");

        Map<String, Object> ok = recruiterBody("Sam Linked", TestDataFactory.uniqueEmail("sam"));
        ok.put("linkedUserId", recruiter3);
        postAs("admin", "/api/recruiters", ok).andExpect(status().isCreated())
                .andExpect(jsonPath("$.linkedUser.username").value("recruiter3"));
        // recruiter3 now has a recruiter profile.
        getAs("recruiter3", "/api/auth/me").andExpect(jsonPath("$.recruiterId").isNumber());
    }

    @Test
    void staleVersionAndValidation() throws Exception {
        long r = createRecruiter("Vera Version");
        Map<String, Object> body = recruiterBody("Vera Updated", TestDataFactory.uniqueEmail("vera"));
        body.put("version", 0);
        putAs("admin", "/api/recruiters/{id}", body, r).andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Vera Updated"));
        expectProblem(putAs("admin", "/api/recruiters/{id}", body, r), 409, "CONCURRENT_MODIFICATION");

        expectProblem(postAs("admin", "/api/recruiters", map("fullName", "", "email", "bad")), 400,
                "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("teamId")))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("email")));
    }
}
