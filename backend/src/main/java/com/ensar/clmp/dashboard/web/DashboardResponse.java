package com.ensar.clmp.dashboard.web;

import java.util.List;

import com.ensar.clmp.history.web.HistoryEntry;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Role-shaped dashboard (contracts/rest-api.md § Dashboard). Sections a role must not see are
 * omitted entirely rather than sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardResponse(Scope scope, String message, List<CountTile> counts,
        List<StatusCount> consultantsByStatus, List<RecruiterPerformanceRow> recruiterPerformance,
        List<HistoryEntry> recentActivity) {

    public enum Scope {
        ORGANIZATION, OWN, CONSULTANT_PIPELINE
    }
}
