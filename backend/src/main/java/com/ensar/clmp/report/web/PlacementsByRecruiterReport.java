package com.ensar.clmp.report.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Placements per recruiter by placement creation date in range (AS 8.2). */
public record PlacementsByRecruiterReport(LocalDate from, LocalDate to, List<Row> rows, Totals totals, boolean empty) {

    public record Row(Long recruiterId, String recruiterName, long placements, Map<String, ReportLink> links) {
    }

    public record Totals(long placements) {
    }
}
