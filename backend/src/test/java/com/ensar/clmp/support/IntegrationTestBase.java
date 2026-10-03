package com.ensar.clmp.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Base for integration tests: full context, MockMvc, the dev seed, and a private in-memory
 * database per test class so state never leaks between classes.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:it-${random.uuid}")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestDataFactory data;

    private final Map<String, MockHttpSession> sessions = new HashMap<>();

    /** Signs in through the real login endpoint and returns the authenticated session. */
    protected MockHttpSession loginAs(String username) {
        return sessions.computeIfAbsent(username, this::freshLogin);
    }

    protected MockHttpSession freshLogin(String username) {
        try {
            MvcResult result = mvc.perform(post("/api/auth/login").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("username", username, "password", "Demo@123"))))
                    .andExpect(status().isOk())
                    .andReturn();
            return (MockHttpSession) result.getRequest().getSession(false);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected String json(Object body) {
        return objectMapper.writeValueAsString(body);
    }

    protected ResultActions getAs(String username, String url, Object... vars) throws Exception {
        return mvc.perform(get(url, vars).session(loginAs(username)));
    }

    protected ResultActions postAs(String username, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(post(url, vars).session(loginAs(username)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body == null ? "{}" : json(body)));
    }

    protected ResultActions putAs(String username, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(put(url, vars).session(loginAs(username)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    protected ResultActions patchAs(String username, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(patch(url, vars).session(loginAs(username)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    protected JsonNode body(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    protected static ResultActions expectProblem(ResultActions actions, int httpStatus, String code)
            throws Exception {
        return actions.andExpect(status().is(httpStatus)).andExpect(jsonPath("$.code").value(code));
    }

    /** A mutable map literal that allows null values (Map.of does not). */
    protected static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }
}
