package com.ensar.clmp.consistency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.ensar.clmp.support.IntegrationTestBase;

import tools.jackson.databind.JsonNode;

/**
 * FR-092 / SC-006: every report cell with a link reproduces its count on the linked list. Runs
 * over the dev seed for ADMIN and MANAGER, for the current month and for all time.
 */
class ReportListConsistencyIT extends IntegrationTestBase {

    private int checked;

    @ParameterizedTest
    @ValueSource(strings = { "admin", "manager" })
    void everyLinkedCellMatchesItsList(String user) throws Exception {
        checked = 0;
        for (String range : new String[] { "", "?from=2026-01-01&to=2026-12-31" }) {
            walk(user, body(getAs(user, "/api/reports/submissions-by-recruiter" + range)));
            walk(user, body(getAs(user, "/api/reports/placements-by-recruiter" + range)));
            walk(user, body(getAs(user, "/api/reports/vendor-client-activity" + range)));
        }
        walk(user, body(getAs(user, "/api/reports/consultant-pipeline")));
        walk(user, body(getAs(user, "/api/reports/bench-ready")));
        assertThat(checked).as("linked cells checked").isGreaterThan(20);
    }

    /** Finds every {@code links} object and compares each linked count with the list total. */
    private void walk(String user, JsonNode node) throws Exception {
        if (node.isArray()) {
            for (JsonNode child : node) {
                walk(user, child);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode links = node.get("links");
        if (links != null && links.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = links.properties().iterator();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                long expected = countFor(node, e.getKey());
                String url = "/api/" + e.getValue().get("list").asText() + "?" + e.getValue().get("query").asText();
                long total = body(getAs(user, url)).get("totalItems").asLong();
                assertThat(total).as("%s %s.%s -> %s", user, node, e.getKey(), url).isEqualTo(expected);
                checked++;
            }
        }
        for (JsonNode child : node) {
            if (child.isObject() || child.isArray()) {
                walk(user, child);
            }
        }
    }

    /** A link key names a numeric field on the same row, or a status inside {@code byStatus}. */
    private static long countFor(JsonNode row, String key) {
        if (row.has(key) && row.get(key).isNumber()) {
            return row.get(key).asLong();
        }
        if (row.has("byStatus") && row.get("byStatus").has(key)) {
            return row.get("byStatus").get(key).asLong();
        }
        throw new AssertionError("no count for link " + key + " in " + row);
    }
}
