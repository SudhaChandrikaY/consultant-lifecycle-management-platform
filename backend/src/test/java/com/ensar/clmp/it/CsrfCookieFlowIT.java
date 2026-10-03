package com.ensar.clmp.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.ensar.clmp.support.IntegrationTestBase;

/**
 * The real SPA CSRF flow (research R5) without spring-security-test's csrf() post-processor,
 * which replaces the token repository for the rest of the context's life. Keep this class free
 * of loginAs()/postAs() helpers for that reason.
 */
class CsrfCookieFlowIT extends IntegrationTestBase {

    @Test
    void spaReadsCookieAndEchoesHeader() throws Exception {
        Cookie xsrf = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();

        String body = json(Map.of("username", "admin", "password", "Demo@123"));

        mvc.perform(post("/api/auth/login").cookie(xsrf).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_AUTHORIZED"));

        mvc.perform(post("/api/auth/login").cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
