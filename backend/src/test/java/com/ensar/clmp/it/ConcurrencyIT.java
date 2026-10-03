package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.support.IntegrationTestBase;
import com.ensar.clmp.support.TestDataFactory;

/** Research R10: stale versions are refused, and per-consultant invariants hold under races. */
class ConcurrencyIT extends IntegrationTestBase {

    private Long readyConsultant(String first) {
        return data.consultant(TestDataFactory.completeProfile(first, "Race", "Java"), ConsultantStatus.READY,
                data.recruiterIdOfUser("recruiter1"));
    }

    private long offer(Long consultant, String client) throws Exception {
        long id = body(postAs("recruiter1", "/api/submissions", map("consultantId", consultant, "vendorName", "Race V",
                "clientName", client, "jobTitle", "Dev", "billRate", 70, "submitNow", true))
                .andExpect(status().isCreated())).get("id").asLong();
        for (String t : new String[] { "INTERVIEW_SCHEDULED", "INTERVIEW_CLEARED", "OFFER" }) {
            long v = body(getAs("admin", "/api/submissions/{id}", id)).get("version").asLong();
            postAs("recruiter1", "/api/submissions/{id}/status", map("targetStatus", t, "version", v), id)
                    .andExpect(status().isOk());
        }
        return id;
    }

    @Test
    void staleVersionsAreRefusedEverywhere() throws Exception {
        Long c = readyConsultant("Stale");
        long v = body(getAs("admin", "/api/consultants/{id}", c)).get("version").asLong();
        expectProblem(putAs("hr", "/api/consultants/{id}", map("firstName", "Stale", "lastName", "Race", "email",
                TestDataFactory.uniqueEmail("stale"), "version", v - 1), c), 409, "CONCURRENT_MODIFICATION");

        Long riya = data.recruiterIdOfUser("recruiter1");
        expectProblem(putAs("admin", "/api/recruiters/{id}", map("fullName", "Riya Patel", "email",
                "riya.patel@clmp.example", "teamId", 1, "regionId", 1, "version", 999), riya), 409,
                "CONCURRENT_MODIFICATION");

        long ma = body(postAs("recruiter1", "/api/marketing-assignments",
                map("consultantId", c, "startDate", "2026-10-01", "targetDate", "2026-11-01"))
                .andExpect(status().isCreated())).get("id").asLong();
        expectProblem(postAs("recruiter1", "/api/marketing-assignments/{id}/transition",
                map("targetStatus", "ACTIVE", "version", 7), ma), 409, "CONCURRENT_MODIFICATION");

        long sub = offer(readyConsultant("Sub"), "Stale Client");
        expectProblem(postAs("recruiter1", "/api/submissions/{id}/status",
                map("targetStatus", "WITHDRAWN", "version", 0), sub), 409, "CONCURRENT_MODIFICATION");

        long pid = body(postAs("recruiter1", "/api/placements", map("submissionId", sub, "startDate", "2026-11-01",
                "billRate", 80, "contractTermMonths", 6)).andExpect(status().isCreated())).get("id").asLong();
        expectProblem(patchAs("admin", "/api/placements/{id}", map("billRate", 90, "version", 5), pid), 409,
                "CONCURRENT_MODIFICATION");
    }

    @Test
    void concurrentMarketingCreatesYieldExactlyOneAssignment() throws Exception {
        Long c = readyConsultant("Twin");
        String body = json(map("consultantId", c, "startDate", "2026-10-01", "targetDate", "2026-11-01"));
        List<Integer> statuses = race("/api/marketing-assignments", body, body);
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
    }

    @Test
    void concurrentPlacementsForOneConsultantYieldExactlyOne() throws Exception {
        Long c = readyConsultant("Dual");
        long first = offer(c, "Client A");
        long second = offer(c, "Client B");
        List<Integer> statuses = race("/api/placements",
                json(map("submissionId", first, "startDate", "2026-11-01", "billRate", 80, "contractTermMonths", 6)),
                json(map("submissionId", second, "startDate", "2026-11-01", "billRate", 80, "contractTermMonths", 6)));
        assertThat(statuses).containsExactlyInAnyOrder(201, 422);
        assertThat(data.consultantStatus(c)).isEqualTo(ConsultantStatus.PLACED);
    }

    /** Fires both POSTs as recruiter1 (separate sessions) at the same moment. */
    private List<Integer> race(String url, String bodyA, String bodyB) throws Exception {
        MockHttpSession a = freshLogin("recruiter1");
        MockHttpSession b = freshLogin("recruiter1");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Map.Entry<MockHttpSession, String> e : List.of(Map.entry(a, bodyA), Map.entry(b, bodyB))) {
                Callable<Integer> call = () -> {
                    start.await();
                    return mvc.perform(post(url).session(e.getKey()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(e.getValue()))
                            .andReturn().getResponse().getStatus();
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : futures) {
                statuses.add(f.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }
}
