package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.ensar.clmp.support.IntegrationTestBase;

/** US1 — Sign in and role-appropriate experience (AS 1.1–1.6, FR-001–FR-007). */
class US1SignInIT extends IntegrationTestBase {

    @Autowired
    Environment environment;

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password))));
    }

    @Test
    void as1_1_meReturnsRoleAndRecruiterLink() throws Exception {
        Long riya = data.recruiterIdOfUser("recruiter1");
        getAs("recruiter1", "/api/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("recruiter1"))
                .andExpect(jsonPath("$.displayName").value("Riya Patel"))
                .andExpect(jsonPath("$.role").value("RECRUITER"))
                .andExpect(jsonPath("$.recruiterId").value(riya))
                .andExpect(jsonPath("$.sessionTimeoutMinutes").value(30));

        getAs("admin", "/api/auth/me")
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.recruiterId").isEmpty());
        getAs("hr", "/api/auth/me").andExpect(jsonPath("$.role").value("HR_OPERATIONS"));
        getAs("manager", "/api/auth/me").andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void as1_1_loginIsCaseInsensitiveOnUsername() throws Exception {
        login("Recruiter1", "Demo@123").andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("recruiter1"));
    }

    @Test
    void as1_2_wrongPasswordAndUnknownUserGetIdenticalGenericError() throws Exception {
        String wrongPassword = expectProblem(login("admin", "nope"), 401, "INVALID_CREDENTIALS")
                .andReturn().getResponse().getContentAsString();
        String unknownUser = expectProblem(login("nobody", "Demo@123"), 401, "INVALID_CREDENTIALS")
                .andReturn().getResponse().getContentAsString();
        assertThat(wrongPassword).isEqualTo(unknownUser);
        assertThat(wrongPassword).contains("Invalid username or password.");
    }

    @Test
    void as1_3_recruiterRefusedFromRecruitersAndReports() throws Exception {
        MvcResult recruiters = expectProblem(getAs("recruiter1", "/api/recruiters"), 403, "NOT_AUTHORIZED")
                .andReturn();
        assertThat(recruiters.getResponse().getContentAsString()).doesNotContain("Riya").doesNotContain("items");
        expectProblem(getAs("recruiter1", "/api/reports/consultant-pipeline"), 403, "NOT_AUTHORIZED");
    }

    @Test
    void as1_4_hrRefusedFromSubmissionAndMarketingCreation() throws Exception {
        expectProblem(postAs("hr", "/api/submissions", Map.of("consultantId", 1)), 403, "NOT_AUTHORIZED");
        expectProblem(postAs("hr", "/api/marketing-assignments", Map.of("consultantId", 1)), 403,
                "NOT_AUTHORIZED");
    }

    @Test
    void as1_5_logoutEndsSessionAndTimeoutIsThirtyMinutes() throws Exception {
        MockHttpSession session = freshLogin("manager");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        expectProblem(mvc.perform(get("/api/auth/me").session(session)), 401, "UNAUTHENTICATED");

        Duration timeout = Binder.get(environment).bind("server.servlet.session.timeout", Duration.class).get();
        assertThat(timeout).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void as1_5_unauthenticatedApiCallsGet401JsonNotRedirect() throws Exception {
        mvc.perform(get("/api/consultants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void as1_6_inactiveUserCannotSignIn() throws Exception {
        String inactive = expectProblem(login("inactive.user", "Demo@123"), 401, "INVALID_CREDENTIALS")
                .andReturn().getResponse().getContentAsString();
        String wrong = login("admin", "bad").andReturn().getResponse().getContentAsString();
        assertThat(inactive).isEqualTo(wrong);
    }

    @Test
    void fr007_noResponseExposesPasswords() throws Exception {
        String loginBody = login("admin", "Demo@123").andReturn().getResponse().getContentAsString();
        String me = getAs("admin", "/api/auth/me").andReturn().getResponse().getContentAsString();
        String failed = login("admin", "Demo@123x").andReturn().getResponse().getContentAsString();
        for (String body : new String[] { loginBody, me, failed }) {
            // The generic failure message itself mentions "password"; no password field or value may appear.
            assertThat(body).doesNotContain("\"password").doesNotContain("passwordHash")
                    .doesNotContain("Demo@123").doesNotContain("$2a$");
        }
    }

    @Test
    void mutatingRequestWithoutCsrfIsRefused() throws Exception {
        mvc.perform(post("/api/auth/logout").session(loginAs("admin"))).andExpect(status().isForbidden());
    }
}
