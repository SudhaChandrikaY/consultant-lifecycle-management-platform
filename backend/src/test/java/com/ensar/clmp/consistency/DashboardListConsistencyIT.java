package com.ensar.clmp.consistency;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.ensar.clmp.support.IntegrationTestBase;

import tools.jackson.databind.JsonNode;

/** AS 7.4 / SC-006 / FR-083: every dashboard figure equals the total of the list it links to. */
class DashboardListConsistencyIT extends IntegrationTestBase {

    @ParameterizedTest
    @ValueSource(strings = { "admin", "manager", "recruiter1", "recruiter2", "hr" })
    void everyCountMatchesItsLinkedList(String user) throws Exception {
        JsonNode dashboard = body(getAs(user, "/api/dashboard"));
        int checked = 0;
        for (String section : new String[] { "counts", "consultantsByStatus" }) {
            JsonNode tiles = dashboard.get(section);
            if (tiles == null || tiles.isNull()) {
                continue;
            }
            for (JsonNode tile : tiles) {
                JsonNode link = tile.get("link");
                long value = tile.has("value") ? tile.get("value").asLong() : tile.get("count").asLong();
                String url = "/api/" + link.get("list").asText() + "?" + link.get("query").asText();
                long total = body(getAs(user, url)).get("totalItems").asLong();
                assertThat(total).as("%s %s -> %s", user, tile, url).isEqualTo(value);
                checked++;
            }
        }
        assertThat(checked).isGreaterThanOrEqualTo(2);
    }
}
