package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ensar.clmp.support.IntegrationTestBase;

import tools.jackson.databind.JsonNode;

/** US7 — Operational dashboard (AS 7.1–7.3, FR-080–FR-082, FR-102). Uses the dev seed. */
class US7DashboardIT extends IntegrationTestBase {

    private static List<String> keys(JsonNode dashboard) {
        List<String> keys = new ArrayList<>();
        dashboard.get("counts").forEach(c -> keys.add(c.get("key").asText()));
        return keys;
    }

    private static long count(JsonNode dashboard, String key) {
        for (JsonNode c : dashboard.get("counts")) {
            if (c.get("key").asText().equals(key)) {
                return c.get("value").asLong();
            }
        }
        throw new AssertionError("missing " + key);
    }

    @Test
    void as7_1_adminAndManagerSeeOrganizationWideFigures() throws Exception {
        JsonNode admin = body(getAs("admin", "/api/dashboard").andExpect(status().isOk()));
        JsonNode manager = body(getAs("manager", "/api/dashboard").andExpect(status().isOk()));
        for (JsonNode d : new JsonNode[] { admin, manager }) {
            assertThat(d.get("scope").asText()).isEqualTo("ORGANIZATION");
            assertThat(keys(d)).containsExactly("BENCH_CONSULTANTS", "READY_CONSULTANTS", "ACTIVE_SUBMISSIONS",
                    "INTERVIEW_SUBMISSIONS", "PLACEMENTS_THIS_MONTH", "NEEDS_REASSIGNMENT");
            assertThat(d.get("recruiterPerformance").size()).isPositive();
            assertThat(d.get("recentActivity").size()).isBetween(1, 20);
            assertThat(d.has("consultantsByStatus")).isFalse();
        }
        assertThat(manager.get("counts")).isEqualTo(admin.get("counts"));
        assertThat(manager.get("recruiterPerformance")).isEqualTo(admin.get("recruiterPerformance"));
        assertThat(count(admin, "PLACEMENTS_THIS_MONTH")).isEqualTo(1);
        assertThat(count(admin, "INTERVIEW_SUBMISSIONS")).isEqualTo(1);

        JsonNode riya = null;
        for (JsonNode row : admin.get("recruiterPerformance")) {
            if (row.get("recruiterName").asText().equals("Riya Patel")) {
                riya = row;
            }
        }
        assertThat(riya).isNotNull();
        assertThat(riya.get("assignedConsultants").asInt()).isPositive();
        assertThat(riya.get("placementsThisMonth").asInt()).isEqualTo(1);

        // Most recent first.
        String first = admin.get("recentActivity").get(0).get("occurredAt").asText();
        String last = admin.get("recentActivity").get(admin.get("recentActivity").size() - 1).get("occurredAt").asText();
        assertThat(first.compareTo(last)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void as7_2_recruiterSeesOwnScopeOnly() throws Exception {
        JsonNode d = body(getAs("recruiter1", "/api/dashboard").andExpect(status().isOk()));
        assertThat(d.get("scope").asText()).isEqualTo("OWN");
        assertThat(keys(d)).containsExactly("BENCH_CONSULTANTS", "READY_CONSULTANTS", "ACTIVE_SUBMISSIONS",
                "INTERVIEW_SUBMISSIONS", "PLACEMENTS_THIS_MONTH");
        assertThat(d.has("recruiterPerformance")).isFalse();
        assertThat(d.has("message")).isFalse();
        assertThat(count(d, "PLACEMENTS_THIS_MONTH")).isEqualTo(1);
        assertThat(count(d, "INTERVIEW_SUBMISSIONS")).isZero();

        // Activity: only rows about recruiter1's consultants or owned by them.
        Set<Long> ownConsultants = new HashSet<>();
        body(getAs("recruiter1", "/api/consultants?size=100")).get("items")
                .forEach(i -> ownConsultants.add(i.get("id").asLong()));
        String activity = d.get("recentActivity").toString();
        assertThat(activity).doesNotContain("Marcus Lee").doesNotContain("Li Wei");
        assertThat(d.get("recentActivity").size()).isPositive();

        JsonNode r2 = body(getAs("recruiter2", "/api/dashboard"));
        assertThat(count(r2, "INTERVIEW_SUBMISSIONS")).isEqualTo(1);
        assertThat(r2.get("recentActivity").toString()).doesNotContain("Riya Patel");
    }

    @Test
    void as7_3_hrSeesConsultantPipelineOnly() throws Exception {
        String raw = getAs("hr", "/api/dashboard").andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("CONSULTANT_PIPELINE"))
                .andExpect(jsonPath("$.consultantsByStatus.length()").value(8))
                .andExpect(jsonPath("$.consultantsByStatus[0].link.list").value("consultants"))
                .andReturn().getResponse().getContentAsString();
        JsonNode d = objectMapper.readTree(raw);
        assertThat(keys(d)).containsExactly("BENCH_CONSULTANTS", "READY_CONSULTANTS");
        assertThat(raw).doesNotContain("billRate").doesNotContain("\"vendor").doesNotContain("\"client")
                .doesNotContain("recruiterPerformance").doesNotContain("Acme").doesNotContain("Globex")
                .doesNotContain("SUBMISSIONS").doesNotContain("PLACEMENTS");
        d.get("recentActivity").forEach(e -> {
            if (e.get("trigger") != null && !e.get("trigger").isNull()) {
                assertThat(e.get("trigger").get("entityType").isNull()).isTrue();
            }
        });
    }

    @Test
    void unlinkedRecruiterGetsZerosAndMessage() throws Exception {
        getAs("recruiter3", "/api/dashboard").andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("OWN"))
                .andExpect(jsonPath("$.message")
                        .value("Your account is not linked to a recruiter profile. Contact an Admin."))
                .andExpect(jsonPath("$.counts[*].value").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(0))))
                .andExpect(jsonPath("$.recentActivity").isEmpty());
    }
}
