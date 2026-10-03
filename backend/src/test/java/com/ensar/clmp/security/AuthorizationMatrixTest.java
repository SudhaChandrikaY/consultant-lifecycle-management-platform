package com.ensar.clmp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.support.IntegrationTestBase;
import com.ensar.clmp.support.TestDataFactory;

import tools.jackson.databind.JsonNode;

/**
 * SC-002 / FR-004: every row of contracts/authorization-matrix.md § Endpoint × Role, for admin,
 * manager, recruiter1 (owned and non-owned records), recruiter3 (unlinked), and hr. Refused cells
 * must return 403 NOT_AUTHORIZED and change nothing (row counts of every table, including
 * history, are compared before and after). Allowed mutating calls are shaped to stop at a
 * business rule after authorization (stale version, duplicate, ineligible state) so fixtures
 * never drift.
 */
class AuthorizationMatrixTest extends IntegrationTestBase {

    private static final String[] TABLES = { "app_user", "recruiter", "consultant", "marketing_assignment",
            "marketing_note", "submission", "submission_note", "submission_duplicate_ref", "placement", "vendor",
            "client", "history_record" };

    @Autowired
    JdbcTemplate jdbc;

    /** Fixture ids, created once for the class. */
    static final Map<String, Long> F = new LinkedHashMap<>();

    record Case(String endpoint, String user, boolean allowed, HttpMethod method,
            Function<AuthorizationMatrixTest, String> url, Function<AuthorizationMatrixTest, Object> body) {

        @Override
        public String toString() {
            return endpoint + " as " + user + (allowed ? " -> allowed" : " -> refused");
        }
    }

    // ---------- fixtures ----------

    @BeforeEach
    void fixtures() throws Exception {
        if (!F.isEmpty()) {
            return;
        }
        Long riya = data.recruiterIdOfUser("recruiter1");
        Long marcus = data.recruiterIdOfUser("recruiter2");
        F.put("riya", riya);
        F.put("ownConsultant", data.consultant(TestDataFactory.completeProfile("Own", "Matrix", "Java"),
                ConsultantStatus.READY, riya));
        F.put("otherConsultant", data.consultant(TestDataFactory.completeProfile("Other", "Matrix", "Java"),
                ConsultantStatus.READY, marcus));

        // An inactive recruiter makes allowed assignment calls stop at RECRUITER_INACTIVE.
        JsonNode inactive = body(postAs("admin", "/api/recruiters", map("fullName", "Matrix Inactive", "email",
                TestDataFactory.uniqueEmail("inactive"), "teamId", teamId(), "regionId", regionId())));
        F.put("inactiveRecruiter", inactive.get("id").asLong());
        postAs("admin", "/api/recruiters/{id}/status", map("status", "INACTIVE", "version", 0),
                inactive.get("id").asLong());

        // Open marketing for each consultant (so allowed creates stop at OPEN_ASSIGNMENT_EXISTS).
        F.put("ownMarketing", createMarketing("recruiter1", F.get("ownConsultant")));
        F.put("otherMarketing", createMarketing("recruiter2", F.get("otherConsultant")));
        transition("recruiter1", F.get("ownMarketing"), "ACTIVE");
        transition("recruiter2", F.get("otherMarketing"), "ACTIVE");

        // Draft and closed assignments whose consultants are Bench: allowed activation/reopen hits 422.
        Long benchForDraft = data.consultant(TestDataFactory.completeProfile("Draft", "Matrix", "Go"),
                ConsultantStatus.READY, riya);
        F.put("draftMarketing", createMarketing("recruiter1", benchForDraft));
        setConsultantStatus(benchForDraft, "BENCH");
        Long benchForClosed = data.consultant(TestDataFactory.completeProfile("Closed", "Matrix", "Go"),
                ConsultantStatus.READY, riya);
        F.put("closedMarketing", createMarketing("recruiter1", benchForClosed));
        transition("recruiter1", F.get("closedMarketing"), "CLOSED");
        setConsultantStatus(benchForClosed, "BENCH");

        // Submissions: own and other; an OFFER by recruiter1 (own submission for placement) and an
        // OFFER by recruiter2 for a consultant now assigned to recruiter1 (consultant-only ownership).
        F.put("ownSubmission", submit("recruiter1", F.get("ownConsultant"), "Matrix Vendor", "Matrix Client"));
        F.put("otherSubmission", submit("recruiter2", F.get("otherConsultant"), "Matrix Vendor", "Matrix Client"));
        Long offerConsultant = data.consultant(TestDataFactory.completeProfile("Offer", "Matrix", "Go"),
                ConsultantStatus.READY, riya);
        F.put("ownOffer", submit("recruiter1", offerConsultant, "Offer Vendor", "Offer Client"));
        moveToOffer("recruiter1", F.get("ownOffer"));
        Long movedConsultant = data.consultant(TestDataFactory.completeProfile("Moved", "Matrix", "Go"),
                ConsultantStatus.READY, marcus);
        F.put("consultantOnlyOffer", submit("recruiter2", movedConsultant, "Moved Vendor", "Moved Client"));
        moveToOffer("recruiter2", F.get("consultantOnlyOffer"));
        postAs("manager", "/api/consultants/{id}/recruiter",
                map("recruiterId", riya, "version", version("/api/consultants/" + movedConsultant)), movedConsultant);

        // Placements: one by recruiter1 (own) and one by recruiter2 (other).
        Long placedOwn = data.consultant(TestDataFactory.completeProfile("Placed", "Own", "Go"),
                ConsultantStatus.READY, riya);
        long s1 = submit("recruiter1", placedOwn, "Place Vendor", "Place Client");
        moveToOffer("recruiter1", s1);
        F.put("ownPlacement", body(postAs("recruiter1", "/api/placements", placement(s1, "2026-11-01")))
                .get("id").asLong());
        Long placedOther = data.consultant(TestDataFactory.completeProfile("Placed", "Other", "Go"),
                ConsultantStatus.READY, marcus);
        long s2 = submit("recruiter2", placedOther, "Place Vendor", "Place Client 2");
        moveToOffer("recruiter2", s2);
        F.put("otherPlacement", body(postAs("recruiter2", "/api/placements", placement(s2, "2026-11-01")))
                .get("id").asLong());
        F.put("vendorId", body(getAs("admin", "/api/submissions/{id}", F.get("ownSubmission"))).get("vendor")
                .get("id").asLong());
    }

    private long teamId() throws Exception {
        return body(getAs("admin", "/api/reference")).get("teams").get(0).get("id").asLong();
    }

    private long regionId() throws Exception {
        return body(getAs("admin", "/api/reference")).get("regions").get(0).get("id").asLong();
    }

    private long createMarketing(String user, Long consultant) throws Exception {
        return body(postAs(user, "/api/marketing-assignments",
                map("consultantId", consultant, "startDate", "2026-10-01", "targetDate", "2026-12-01"))).get("id")
                .asLong();
    }

    private void transition(String user, long id, String target) throws Exception {
        postAs(user, "/api/marketing-assignments/{id}/transition",
                map("targetStatus", target, "version", version("/api/marketing-assignments/" + id)), id);
    }

    private void setConsultantStatus(Long id, String target) throws Exception {
        postAs("hr", "/api/consultants/{id}/status",
                map("targetStatus", target, "version", version("/api/consultants/" + id)), id);
    }

    private long submit(String user, Long consultant, String vendor, String client) throws Exception {
        return body(postAs(user, "/api/submissions", map("consultantId", consultant, "vendorName", vendor,
                "clientName", client, "jobTitle", "Matrix Job", "billRate", 75, "submitNow", true,
                "submittedDate", "2026-10-10"))).get("id").asLong();
    }

    private void moveToOffer(String user, long id) throws Exception {
        for (String t : new String[] { "INTERVIEW_SCHEDULED", "INTERVIEW_CLEARED", "OFFER" }) {
            postAs(user, "/api/submissions/{id}/status",
                    map("targetStatus", t, "version", version("/api/submissions/" + id)), id);
        }
    }

    private static Map<String, Object> placement(long submissionId, String startDate) {
        return map("submissionId", submissionId, "startDate", startDate, "billRate", 80, "contractTermMonths", 6);
    }

    long version(String url) {
        try {
            return body(getAs("admin", url)).get("version").asLong();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- the matrix ----------

    static Stream<Case> matrix() {
        List<Case> cases = new ArrayList<>();
        Builder b = new Builder(cases);

        b.get("GET /auth/me", t -> "/api/auth/me").allow("admin", "manager", "recruiter1", "recruiter3", "hr");
        b.get("GET /auth/csrf", t -> "/api/auth/csrf").allow("admin", "manager", "recruiter1", "hr");
        b.get("GET /reference", t -> "/api/reference").allow("admin", "manager", "recruiter1", "hr");
        b.get("GET /vendors", t -> "/api/vendors").allow("admin", "manager", "recruiter1").deny("hr");
        b.get("GET /clients", t -> "/api/clients").allow("admin", "manager", "recruiter1").deny("hr");
        b.get("GET /dashboard", t -> "/api/dashboard").allow("admin", "manager", "recruiter1", "recruiter3", "hr");

        // Recruiters
        b.get("GET /recruiters", t -> "/api/recruiters").allow("admin", "manager").deny("recruiter1", "hr");
        b.get("GET /recruiters/{id}", t -> "/api/recruiters/" + F.get("riya")).allow("admin", "manager")
                .deny("recruiter1", "hr");
        b.send("POST /recruiters", HttpMethod.POST, t -> "/api/recruiters",
                t -> map("fullName", "Dup", "email", "riya.patel@clmp.example", "teamId", 1, "regionId", 1))
                .allow("admin").deny("manager", "recruiter1", "hr");
        b.send("PUT /recruiters/{id}", HttpMethod.PUT, t -> "/api/recruiters/" + F.get("riya"),
                t -> map("fullName", "Riya Patel", "email", "riya.patel@clmp.example", "teamId", 1, "regionId", 1,
                        "version", -1))
                .allow("admin").deny("manager", "recruiter1", "hr");
        b.send("POST /recruiters/{id}/status", HttpMethod.POST, t -> "/api/recruiters/" + F.get("riya") + "/status",
                t -> map("status", "INACTIVE", "version", -1))
                .allow("admin").deny("manager", "recruiter1", "hr");
        b.get("GET /recruiters/linkable-users", t -> "/api/recruiters/linkable-users").allow("admin")
                .deny("manager", "recruiter1", "hr");

        // Consultants
        b.get("GET /consultants", t -> "/api/consultants").allow("admin", "manager", "recruiter1", "recruiter3", "hr");
        b.get("GET /consultants/skills", t -> "/api/consultants/skills").allow("admin", "manager", "recruiter1", "hr");
        b.get("GET /consultants/{own}", t -> "/api/consultants/" + F.get("ownConsultant"))
                .allow("admin", "manager", "recruiter1", "hr").deny("recruiter3");
        b.get("GET /consultants/{other}", t -> "/api/consultants/" + F.get("otherConsultant")).deny("recruiter1");
        b.get("GET /consultants/{own}/history", t -> "/api/consultants/" + F.get("ownConsultant") + "/history")
                .allow("admin", "manager", "recruiter1", "hr");
        b.get("GET /consultants/{other}/history", t -> "/api/consultants/" + F.get("otherConsultant") + "/history")
                .deny("recruiter1");
        b.send("POST /consultants", HttpMethod.POST, t -> "/api/consultants",
                t -> map("firstName", "Dup", "lastName", "Email", "email", "meera.iyer@consultants.example"))
                .allow("admin", "hr").deny("manager", "recruiter1");
        b.send("PUT /consultants/{id}", HttpMethod.PUT, t -> "/api/consultants/" + F.get("ownConsultant"),
                t -> map("firstName", "Own", "lastName", "Matrix", "email", "own.stale@test.example", "version", -1))
                .allow("admin", "hr").deny("manager", "recruiter1");
        b.send("POST /consultants/{id}/status", HttpMethod.POST,
                t -> "/api/consultants/" + F.get("ownConsultant") + "/status",
                t -> map("targetStatus", "MARKETING", "version", t.version("/api/consultants/" + F.get("ownConsultant"))))
                .allow("admin", "hr").deny("manager", "recruiter1");
        b.send("POST /consultants/{id}/recruiter", HttpMethod.POST,
                t -> "/api/consultants/" + F.get("ownConsultant") + "/recruiter",
                t -> map("recruiterId", F.get("inactiveRecruiter"),
                        "version", t.version("/api/consultants/" + F.get("ownConsultant"))))
                .allow("admin", "manager").deny("recruiter1", "hr");

        // Marketing
        b.get("GET /marketing-assignments", t -> "/api/marketing-assignments")
                .allow("admin", "manager", "recruiter1").deny("hr");
        b.get("GET /marketing-assignments/{own}", t -> "/api/marketing-assignments/" + F.get("ownMarketing"))
                .allow("admin", "manager", "recruiter1").deny("hr", "recruiter3");
        b.get("GET /marketing-assignments/{other}", t -> "/api/marketing-assignments/" + F.get("otherMarketing"))
                .deny("recruiter1");
        b.get("GET /marketing-assignments/{own}/history",
                t -> "/api/marketing-assignments/" + F.get("ownMarketing") + "/history")
                .allow("admin", "manager", "recruiter1").deny("hr");
        b.send("POST /marketing-assignments (own)", HttpMethod.POST, t -> "/api/marketing-assignments",
                t -> map("consultantId", F.get("ownConsultant"), "startDate", "2026-10-01", "targetDate", "2026-12-01"))
                .allow("admin", "recruiter1").deny("manager", "hr", "recruiter3");
        b.send("POST /marketing-assignments (other)", HttpMethod.POST, t -> "/api/marketing-assignments",
                t -> map("consultantId", F.get("otherConsultant"), "startDate", "2026-10-01", "targetDate",
                        "2026-12-01"))
                .deny("recruiter1");
        b.send("PUT /marketing-assignments/{own}", HttpMethod.PUT,
                t -> "/api/marketing-assignments/" + F.get("ownMarketing"),
                t -> map("startDate", "2026-10-01", "targetDate", "2026-12-01", "version", -1))
                .allow("admin", "recruiter1").deny("manager", "hr");
        b.send("PUT /marketing-assignments/{other}", HttpMethod.PUT,
                t -> "/api/marketing-assignments/" + F.get("otherMarketing"),
                t -> map("startDate", "2026-10-01", "targetDate", "2026-12-01", "version", -1))
                .deny("recruiter1");
        b.send("POST /marketing-assignments/{own}/notes", HttpMethod.POST,
                t -> "/api/marketing-assignments/" + F.get("ownMarketing") + "/notes", t -> map("body", "Matrix note"))
                .allow("admin", "recruiter1").deny("manager", "hr");
        b.send("POST /marketing-assignments/{other}/notes", HttpMethod.POST,
                t -> "/api/marketing-assignments/" + F.get("otherMarketing") + "/notes", t -> map("body", "x"))
                .deny("recruiter1");
        b.send("transition DRAFT->ACTIVE", HttpMethod.POST,
                t -> "/api/marketing-assignments/" + F.get("draftMarketing") + "/transition",
                t -> map("targetStatus", "ACTIVE", "version",
                        t.version("/api/marketing-assignments/" + F.get("draftMarketing"))))
                .allow("admin", "recruiter1").deny("manager", "hr");
        b.send("transition ACTIVE->HOLD", HttpMethod.POST,
                t -> "/api/marketing-assignments/" + F.get("ownMarketing") + "/transition",
                t -> map("targetStatus", "HOLD", "version",
                        t.version("/api/marketing-assignments/" + F.get("ownMarketing"))))
                .allow("admin", "manager", "recruiter1").deny("hr");
        b.send("transition ACTIVE->HOLD (other)", HttpMethod.POST,
                t -> "/api/marketing-assignments/" + F.get("otherMarketing") + "/transition",
                t -> map("targetStatus", "HOLD", "reason", "x", "version",
                        t.version("/api/marketing-assignments/" + F.get("otherMarketing"))))
                .deny("recruiter1");
        b.send("transition CLOSED->ACTIVE", HttpMethod.POST,
                t -> "/api/marketing-assignments/" + F.get("closedMarketing") + "/transition",
                t -> map("targetStatus", "ACTIVE", "version",
                        t.version("/api/marketing-assignments/" + F.get("closedMarketing"))))
                .allow("admin", "manager").deny("recruiter1", "hr");

        // Submissions
        b.get("GET /submissions", t -> "/api/submissions").allow("admin", "manager", "recruiter1").deny("hr");
        b.get("GET /submissions/{own}", t -> "/api/submissions/" + F.get("ownSubmission"))
                .allow("admin", "manager", "recruiter1").deny("hr", "recruiter3");
        b.get("GET /submissions/{consultant-only own}", t -> "/api/submissions/" + F.get("consultantOnlyOffer"))
                .allow("recruiter1");
        b.get("GET /submissions/{other}", t -> "/api/submissions/" + F.get("otherSubmission")).deny("recruiter1");
        b.send("POST /submissions (own consultant)", HttpMethod.POST, t -> "/api/submissions",
                t -> map("consultantId", F.get("ownConsultant"), "vendorName", "Matrix Vendor", "clientName",
                        "Matrix Client", "jobTitle", "Matrix Job", "billRate", 75, "submitNow", true))
                .allow("admin", "recruiter1").deny("manager", "hr", "recruiter3");
        b.send("POST /submissions (other consultant)", HttpMethod.POST, t -> "/api/submissions",
                t -> map("consultantId", F.get("otherConsultant"), "vendorName", "Matrix Vendor", "clientName",
                        "Matrix Client", "jobTitle", "Matrix Job", "billRate", 75, "submitNow", true))
                .deny("recruiter1");
        b.send("POST /submissions/{own}/status", HttpMethod.POST,
                t -> "/api/submissions/" + F.get("ownSubmission") + "/status",
                t -> map("targetStatus", "PLACED", "version", t.version("/api/submissions/" + F.get("ownSubmission"))))
                .allow("admin", "recruiter1").deny("manager", "hr");
        b.send("POST /submissions/{other}/status", HttpMethod.POST,
                t -> "/api/submissions/" + F.get("otherSubmission") + "/status",
                t -> map("targetStatus", "PLACED", "version", t.version("/api/submissions/" + F.get("otherSubmission"))))
                .deny("recruiter1");
        b.send("POST /submissions/{own}/notes", HttpMethod.POST,
                t -> "/api/submissions/" + F.get("ownSubmission") + "/notes", t -> map("body", "Matrix note"))
                .allow("admin", "recruiter1").deny("manager", "hr");
        b.send("POST /submissions/{other}/notes", HttpMethod.POST,
                t -> "/api/submissions/" + F.get("otherSubmission") + "/notes", t -> map("body", "x"))
                .deny("recruiter1");
        b.send("PUT /submissions/{id} is not exposed", HttpMethod.PUT,
                t -> "/api/submissions/" + F.get("ownSubmission"), t -> map("jobTitle", "x"))
                .allow("admin", "recruiter1").deny("hr");

        // Placements
        b.get("GET /placements", t -> "/api/placements").allow("admin", "manager", "recruiter1").deny("hr");
        b.get("GET /placements/{own}", t -> "/api/placements/" + F.get("ownPlacement"))
                .allow("admin", "manager", "recruiter1").deny("hr", "recruiter3");
        b.get("GET /placements/{other}", t -> "/api/placements/" + F.get("otherPlacement")).deny("recruiter1");
        b.get("GET /placements/draft (own submission)", t -> "/api/placements/draft?submissionId=" + F.get("ownOffer"))
                .allow("admin", "recruiter1").deny("manager", "hr");
        b.get("GET /placements/draft (consultant-only)",
                t -> "/api/placements/draft?submissionId=" + F.get("consultantOnlyOffer")).deny("recruiter1");
        b.send("POST /placements (own submission)", HttpMethod.POST, t -> "/api/placements",
                t -> placement(F.get("ownOffer"), "2026-01-01"))
                .allow("admin", "recruiter1").deny("manager", "hr");
        b.send("POST /placements (consultant-only)", HttpMethod.POST, t -> "/api/placements",
                t -> placement(F.get("consultantOnlyOffer"), "2026-01-01")).deny("recruiter1");
        b.send("PATCH /placements/{id}", HttpMethod.PATCH, t -> "/api/placements/" + F.get("ownPlacement"),
                t -> map("billRate", 99, "version", -1))
                .allow("admin").deny("manager", "recruiter1", "hr");

        // Reports and the H2 console
        for (String r : new String[] { "submissions-by-recruiter", "placements-by-recruiter", "consultant-pipeline",
                "bench-ready", "vendor-client-activity" }) {
            b.get("GET /reports/" + r, t -> "/api/reports/" + r).allow("admin", "manager").deny("recruiter1", "hr");
        }
        b.get("GET /h2-console", t -> "/h2-console/").allow("admin").deny("manager", "recruiter1", "hr");
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matrix")
    void cell(Case c) throws Exception {
        Map<String, Long> before = rowCounts();
        MockHttpServletRequestBuilder req = request(c.method(), c.url().apply(this)).session(loginAs(c.user()));
        if (c.method() != HttpMethod.GET) {
            req = req.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(c.body().apply(this)));
        }
        MockHttpServletResponse response = mvc.perform(req).andReturn().getResponse();

        if (c.allowed()) {
            assertThat(response.getStatus()).as("%s body %s", c, response.getContentAsString()).isNotEqualTo(403);
            if (c.method() == HttpMethod.GET && c.url().apply(this).startsWith("/api/")) {
                assertThat(response.getStatus()).as("%s", c).isIn(200, 204);
            }
        } else {
            assertThat(response.getStatus()).as("%s body %s", c, response.getContentAsString()).isEqualTo(403);
            assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
                    .isEqualTo("NOT_AUTHORIZED");
            assertThat(rowCounts()).as("no state change for %s", c).isEqualTo(before);
        }
    }

    private Map<String, Long> rowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : TABLES) {
            counts.put(table, jdbc.queryForObject("select count(*) from " + table, Long.class));
        }
        counts.put("consultant_versions", jdbc.queryForObject("select coalesce(sum(version),0) from consultant", Long.class));
        counts.put("marketing_versions",
                jdbc.queryForObject("select coalesce(sum(version),0) from marketing_assignment", Long.class));
        counts.put("submission_versions", jdbc.queryForObject("select coalesce(sum(version),0) from submission", Long.class));
        counts.put("recruiter_versions", jdbc.queryForObject("select coalesce(sum(version),0) from recruiter", Long.class));
        counts.put("placement_versions", jdbc.queryForObject("select coalesce(sum(version),0) from placement", Long.class));
        return counts;
    }

    /** Small fluent helper that expands one matrix row into per-user cases. */
    static final class Builder {
        private final List<Case> cases;

        Builder(List<Case> cases) {
            this.cases = cases;
        }

        Row get(String endpoint, Function<AuthorizationMatrixTest, String> url) {
            return new Row(endpoint, HttpMethod.GET, url, t -> null);
        }

        Row send(String endpoint, HttpMethod method, Function<AuthorizationMatrixTest, String> url,
                Function<AuthorizationMatrixTest, Object> body) {
            return new Row(endpoint, method, url, body);
        }

        final class Row {
            private final String endpoint;
            private final HttpMethod method;
            private final Function<AuthorizationMatrixTest, String> url;
            private final Function<AuthorizationMatrixTest, Object> body;

            Row(String endpoint, HttpMethod method, Function<AuthorizationMatrixTest, String> url,
                    Function<AuthorizationMatrixTest, Object> body) {
                this.endpoint = endpoint;
                this.method = method;
                this.url = url;
                this.body = body;
            }

            Row allow(String... users) {
                for (String u : users) {
                    cases.add(new Case(endpoint, u, true, method, url, body));
                }
                return this;
            }

            Row deny(String... users) {
                for (String u : users) {
                    cases.add(new Case(endpoint, u, false, method, url, body));
                }
                return this;
            }
        }
    }
}
