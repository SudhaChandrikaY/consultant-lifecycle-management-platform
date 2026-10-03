package com.ensar.clmp.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.ensar.clmp.support.IntegrationTestBase;

/**
 * SC-007: lists, the dashboard, and every report respond in under 2 s with 500 consultants, 50
 * recruiters, and 2,000 submissions. Excluded from the default build; run with
 * {@code ./mvnw verify -Dgroups=perf}.
 */
@Tag("perf")
@ActiveProfiles({ "dev", "perf" })
class PerformanceSmokeIT extends IntegrationTestBase {

    private static final long LIMIT_MS = 2000;

    @Test
    void listsDashboardAndReportsRespondWithinTwoSeconds() throws Exception {
        assertThat(body(getAs("admin", "/api/consultants?size=1")).get("totalItems").asLong()).isGreaterThanOrEqualTo(500);
        assertThat(body(getAs("admin", "/api/submissions?size=1")).get("totalItems").asLong()).isGreaterThanOrEqualTo(2000);
        assertThat(body(getAs("admin", "/api/recruiters?size=1")).get("totalItems").asLong()).isGreaterThanOrEqualTo(50);

        String[] urls = { "/api/consultants?size=100", "/api/consultants?status=READY&status=MARKETING&size=100",
                "/api/submissions?size=100", "/api/submissions?status=OFFER&sort=billRate,desc&size=100",
                "/api/placements?size=100", "/api/recruiters?size=100", "/api/marketing-assignments?size=100",
                "/api/dashboard", "/api/reports/submissions-by-recruiter?from=2026-01-01&to=2026-12-31",
                "/api/reports/placements-by-recruiter?from=2026-01-01&to=2026-12-31",
                "/api/reports/consultant-pipeline", "/api/reports/bench-ready",
                "/api/reports/vendor-client-activity?from=2026-01-01&to=2026-12-31" };
        for (String url : urls) {
            getAs("admin", url); // warm-up
            long start = System.nanoTime();
            getAs("admin", url).andExpect(status().isOk());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertThat(elapsedMs).as("%s took %d ms", url, elapsedMs).isLessThan(LIMIT_MS);
        }
        // Also as a recruiter (scoped queries with joins).
        long start = System.nanoTime();
        getAs("recruiter1", "/api/dashboard").andExpect(status().isOk());
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(LIMIT_MS);
    }
}
