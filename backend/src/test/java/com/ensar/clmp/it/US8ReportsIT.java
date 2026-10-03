package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.support.IntegrationTestBase;
import com.ensar.clmp.support.TestDataFactory;

import tools.jackson.databind.JsonNode;

/** US8 — Operational reports (AS 8.1–8.5, FR-090–FR-093). March 2026 data is created by this test only. */
class US8ReportsIT extends IntegrationTestBase {

    private static final String MARCH = "from=2026-03-01&to=2026-03-31";
    private static boolean seeded;
    private static long placementSubmission;

    private long submit(String user, Long consultant, String vendor, String client, String date) throws Exception {
        return body(postAs(user, "/api/submissions", map("consultantId", consultant, "vendorName", vendor,
                "clientName", client, "jobTitle", "Report Dev " + date + vendor, "billRate", 70, "submitNow", true,
                "submittedDate", date)).andExpect(status().isCreated())).get("id").asLong();
    }

    private void move(String user, long id, String... targets) throws Exception {
        for (String t : targets) {
            long v = body(getAs("admin", "/api/submissions/{id}", id)).get("version").asLong();
            postAs(user, "/api/submissions/{id}/status", map("targetStatus", t, "version", v), id)
                    .andExpect(status().isOk());
        }
    }

    @BeforeEach
    void seedMarch() throws Exception {
        if (seeded) {
            return;
        }
        Long riyaConsultant = data.consultant(TestDataFactory.completeProfile("Rep", "One", "Java"),
                ConsultantStatus.READY, data.recruiterIdOfUser("recruiter1"));
        Long marcusConsultant = data.consultant(TestDataFactory.completeProfile("Rep", "Two", "Java"),
                ConsultantStatus.READY, data.recruiterIdOfUser("recruiter2"));
        long a = submit("recruiter1", riyaConsultant, "Report Vendor", "Report Client", "2026-03-05");
        long b = submit("recruiter1", riyaConsultant, "Report Vendor", "Report Client", "2026-03-06");
        submit("recruiter1", riyaConsultant, "Report Vendor", "Other Client", "2026-03-07");
        long c = submit("recruiter2", marcusConsultant, "Second Vendor", "Report Client", "2026-03-20");
        move("recruiter1", a, "INTERVIEW_SCHEDULED");
        move("recruiter1", b, "INTERVIEW_SCHEDULED", "INTERVIEW_CLEARED");
        move("recruiter2", c, "UNDER_REVIEW", "INTERVIEW_SCHEDULED", "INTERVIEW_CLEARED", "OFFER");
        placementSubmission = c;
        postAs("recruiter2", "/api/placements", map("submissionId", c, "startDate", "2026-11-01", "billRate", 80,
                "contractTermMonths", 6)).andExpect(status().isCreated());
        seeded = true;
    }

    private static JsonNode row(JsonNode rows, String field, String value) {
        for (JsonNode r : rows) {
            if (r.get(field).asText().equals(value)) {
                return r;
            }
        }
        throw new AssertionError("no row " + value);
    }

    @Test
    void as8_1_submissionsByRecruiterBySubmittedDateWithCurrentStatus() throws Exception {
        JsonNode report = body(getAs("manager", "/api/reports/submissions-by-recruiter?" + MARCH)
                .andExpect(status().isOk()));
        assertThat(report.get("from").asText()).isEqualTo("2026-03-01");
        assertThat(report.get("empty").asBoolean()).isFalse();
        JsonNode riya = row(report.get("rows"), "recruiterName", "Riya Patel");
        assertThat(riya.get("total").asInt()).isEqualTo(3);
        assertThat(riya.get("byStatus").get("SUBMITTED").asInt()).isEqualTo(1);
        assertThat(riya.get("byStatus").get("INTERVIEW_SCHEDULED").asInt()).isEqualTo(1);
        assertThat(riya.get("byStatus").get("INTERVIEW_CLEARED").asInt()).isEqualTo(1);
        JsonNode marcus = row(report.get("rows"), "recruiterName", "Marcus Lee");
        assertThat(marcus.get("byStatus").get("PLACED").asInt()).isEqualTo(1);
        assertThat(report.get("totals").get("total").asInt()).isEqualTo(4);
    }

    @Test
    void as8_2_placementsByRecruiterByCreationDate() throws Exception {
        JsonNode march = body(getAs("admin", "/api/reports/placements-by-recruiter?" + MARCH));
        assertThat(march.get("empty").asBoolean()).isTrue();
        JsonNode october = body(getAs("admin",
                "/api/reports/placements-by-recruiter?from=2026-10-01&to=2026-10-31"));
        JsonNode marcus = row(october.get("rows"), "recruiterName", "Marcus Lee");
        assertThat(marcus.get("placements").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode riya = row(october.get("rows"), "recruiterName", "Riya Patel");
        assertThat(riya.get("placements").asInt()).isEqualTo(1); // seeded Vikram Rao
    }

    @Test
    void as8_3_pipelineHasAllStatusesIncludingZeros() throws Exception {
        JsonNode report = body(getAs("admin", "/api/reports/consultant-pipeline"));
        assertThat(report.get("rows")).hasSize(8);
        int sum = 0;
        for (JsonNode r : report.get("rows")) {
            sum += r.get("count").asInt();
        }
        assertThat(report.get("total").asInt()).isEqualTo(sum);
        JsonNode benchReady = body(getAs("manager", "/api/reports/bench-ready"));
        assertThat(benchReady.get("asOf").asText()).isEqualTo("2026-10-15");
        assertThat(benchReady.get("bench").asInt()).isEqualTo(row(report.get("rows"), "status", "BENCH").get("count").asInt());
        assertThat(benchReady.get("ready").asInt()).isEqualTo(row(report.get("rows"), "status", "READY").get("count").asInt());
    }

    @Test
    void as8_4_vendorClientActivity() throws Exception {
        JsonNode report = body(getAs("admin", "/api/reports/vendor-client-activity?" + MARCH));
        JsonNode vendor = row(report.get("vendors"), "name", "Report Vendor");
        assertThat(vendor.get("submissions").asInt()).isEqualTo(3);
        // Current status INTERVIEW_SCHEDULED only: the one that moved on to Interview Cleared is not counted.
        assertThat(vendor.get("interviewsScheduled").asInt()).isEqualTo(1);
        assertThat(vendor.get("placements").asInt()).isZero();
        JsonNode client = row(report.get("clients"), "name", "Report Client");
        assertThat(client.get("submissions").asInt()).isEqualTo(3);
        assertThat(client.get("interviewsScheduled").asInt()).isEqualTo(1);

        // Placements count by creation date: the Second Vendor placement was created on 2026-10-15.
        JsonNode october = body(getAs("admin", "/api/reports/vendor-client-activity?from=2026-10-01&to=2026-10-31"));
        JsonNode second = row(october.get("vendors"), "name", "Second Vendor");
        assertThat(second.get("placements").asInt()).isEqualTo(1);
        assertThat(second.get("submissions").asInt()).isZero();
        assertThat(placementSubmission).isPositive();
    }

    @Test
    void as8_5_emptyRangeIsFlagged() throws Exception {
        String range = "from=2020-01-01&to=2020-01-31";
        for (String report : new String[] { "submissions-by-recruiter", "placements-by-recruiter",
                "vendor-client-activity" }) {
            getAs("admin", "/api/reports/" + report + "?" + range).andExpect(status().isOk())
                    .andExpect(jsonPath("$.empty").value(true));
        }
    }

    @Test
    void fr091_defaultsToCurrentMonthAndValidatesRange() throws Exception {
        getAs("admin", "/api/reports/submissions-by-recruiter").andExpect(jsonPath("$.from").value("2026-10-01"))
                .andExpect(jsonPath("$.to").value("2026-10-31"));
        getAs("admin", "/api/reports/vendor-client-activity").andExpect(jsonPath("$.from").value("2026-10-01"));
        expectProblem(getAs("admin", "/api/reports/placements-by-recruiter?from=2026-10-31&to=2026-10-01"), 400,
                "VALIDATION_FAILED").andExpect(jsonPath("$.fieldErrors[0].field").value("to"));
    }

    @Test
    void recruiterAndHrAreRefused() throws Exception {
        for (String user : new String[] { "recruiter1", "hr" }) {
            for (String report : new String[] { "submissions-by-recruiter", "placements-by-recruiter",
                    "consultant-pipeline", "bench-ready", "vendor-client-activity" }) {
                expectProblem(getAs(user, "/api/reports/" + report), 403, "NOT_AUTHORIZED");
            }
        }
    }
}
