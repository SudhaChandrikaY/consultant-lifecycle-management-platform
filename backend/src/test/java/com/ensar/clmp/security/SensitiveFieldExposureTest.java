package com.ensar.clmp.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ensar.clmp.support.IntegrationTestBase;

import tools.jackson.databind.JsonNode;

/**
 * FR-007, FR-024, FR-064, FR-103 over the dev seed: walks every readable GET endpoint for each
 * role and checks that passwords, contact details, and commercial data never leak.
 */
class SensitiveFieldExposureTest extends IntegrationTestBase {

    private static final List<String> SEEDED_COUNTERPARTIES = List.of("Acme Staffing", "TechBridge Partners",
            "Nimbus Talent", "Globex", "Initech", "Umbrella Health");
    private static final Set<String> COMMERCIAL_KEYS = Set.of("vendor", "vendorName", "client", "clientName",
            "billRate", "contractTermMonths", "submissions", "placements", "currentMarketingAssignment",
            "openSubmissionsWhileOnHold", "recruiterPerformance");

    private List<Long> ids(String user, String list) throws Exception {
        List<Long> ids = new ArrayList<>();
        body(getAs(user, list + (list.contains("?") ? "&" : "?") + "size=100")).get("items")
                .forEach(i -> ids.add(i.get("id").asLong()));
        return ids;
    }

    /** Every GET the user may call, with ids taken from what the user can list. */
    private List<String> readableUrls(String user) throws Exception {
        List<String> urls = new ArrayList<>(List.of("/api/auth/me", "/api/reference", "/api/dashboard",
                "/api/consultants?size=100", "/api/consultants/skills"));
        for (Long id : ids(user, "/api/consultants")) {
            urls.add("/api/consultants/" + id);
            urls.add("/api/consultants/" + id + "/history?size=100");
        }
        if (!user.equals("hr")) {
            urls.addAll(List.of("/api/vendors", "/api/clients", "/api/marketing-assignments?size=100",
                    "/api/submissions?size=100", "/api/placements?size=100"));
            for (Long id : ids(user, "/api/marketing-assignments")) {
                urls.add("/api/marketing-assignments/" + id);
                urls.add("/api/marketing-assignments/" + id + "/history?size=100");
            }
            for (Long id : ids(user, "/api/submissions")) {
                urls.add("/api/submissions/" + id);
            }
            for (Long id : ids(user, "/api/placements")) {
                urls.add("/api/placements/" + id);
            }
        }
        if (user.equals("admin") || user.equals("manager")) {
            urls.addAll(List.of("/api/recruiters?size=100", "/api/reports/submissions-by-recruiter?from=2026-01-01",
                    "/api/reports/placements-by-recruiter?from=2026-01-01", "/api/reports/consultant-pipeline",
                    "/api/reports/bench-ready", "/api/reports/vendor-client-activity?from=2026-01-01&to=2026-12-31"));
            for (Long id : ids(user, "/api/recruiters")) {
                urls.add("/api/recruiters/" + id);
            }
        }
        return urls;
    }

    @Test
    void noRoleEverSeesPasswords() throws Exception {
        for (String user : new String[] { "admin", "manager", "recruiter1", "recruiter2", "recruiter3", "hr" }) {
            for (String url : readableUrls(user)) {
                String raw = getAs(user, url).andReturn().getResponse().getContentAsString();
                Set<String> keys = keys(objectMapper.readTree(raw));
                assertThat(keys).as("%s %s", user, url).doesNotContain("password", "passwordHash");
                assertThat(raw).as("%s %s", user, url).doesNotContain("$2a$").doesNotContain("Demo@123");
            }
        }
    }

    @Test
    void consultantListsNeverCarryContactFields() throws Exception {
        for (String user : new String[] { "admin", "manager", "recruiter1", "hr" }) {
            JsonNode page = body(getAs(user, "/api/consultants?size=100"));
            assertThat(page.get("items").size()).isPositive();
            for (JsonNode item : page.get("items")) {
                assertThat(keys(item)).as(user).doesNotContain("email", "phone", "visaExpirationDate", "notes",
                        "contact");
            }
        }
    }

    @Test
    void hrSeesNoCommercialData() throws Exception {
        for (String url : readableUrls("hr")) {
            String raw = getAs("hr", url).andReturn().getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(raw);
            assertThat(keys(json)).as(url).doesNotContainAnyElementsOf(COMMERCIAL_KEYS);
            for (String name : SEEDED_COUNTERPARTIES) {
                assertThat(raw).as("%s mentions %s", url, name).doesNotContain(name);
            }
        }
    }

    @Test
    void historyNeverRecordsContactValues() throws Exception {
        Set<String> secrets = new HashSet<>();
        for (Long id : ids("admin", "/api/consultants")) {
            JsonNode contact = body(getAs("admin", "/api/consultants/" + id)).get("contact");
            secrets.add(contact.get("email").asText());
            if (contact.hasNonNull("phone")) {
                secrets.add(contact.get("phone").asText());
            }
        }
        assertThat(secrets).isNotEmpty();
        List<String> urls = new ArrayList<>();
        for (Long id : ids("admin", "/api/consultants")) {
            urls.add("/api/consultants/" + id + "/history?size=100");
        }
        for (Long id : ids("admin", "/api/marketing-assignments")) {
            urls.add("/api/marketing-assignments/" + id + "/history?size=100");
        }
        for (Long id : ids("admin", "/api/submissions")) {
            urls.add("/api/submissions/" + id);
        }
        for (Long id : ids("admin", "/api/placements")) {
            urls.add("/api/placements/" + id);
        }
        urls.add("/api/dashboard");
        for (String url : urls) {
            JsonNode json = body(getAs("admin", url));
            List<JsonNode> entries = new ArrayList<>();
            collectHistory(json, entries);
            for (JsonNode e : entries) {
                for (String field : new String[] { "oldValue", "newValue", "field", "description", "reason" }) {
                    String value = e.hasNonNull(field) ? e.get(field).asText() : "";
                    for (String secret : secrets) {
                        assertThat(value).as("%s %s", url, field).doesNotContain(secret);
                    }
                }
            }
        }
    }

    private static void collectHistory(JsonNode node, List<JsonNode> out) {
        if (node.isObject() && node.has("changeType") && node.has("occurredAt")) {
            out.add(node);
        }
        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                collectHistory(child, out);
            }
        }
    }

    private static Set<String> keys(JsonNode node) {
        Set<String> keys = new HashSet<>();
        collectKeys(node, keys);
        return keys;
    }

    private static void collectKeys(JsonNode node, Set<String> keys) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.properties().iterator();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                keys.add(e.getKey());
                collectKeys(e.getValue(), keys);
            }
        } else if (node.isArray()) {
            node.forEach(child -> collectKeys(child, keys));
        }
    }
}
