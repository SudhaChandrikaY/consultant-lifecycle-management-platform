package com.ensar.clmp.report.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Per vendor and per client (AS 8.4): submissions and interviews scheduled by submitted date in
 * range (interviews = submissions currently Interview Scheduled), placements by creation date.
 */
public record VendorClientActivityReport(LocalDate from, LocalDate to, List<Row> vendors, List<Row> clients,
        boolean empty) {

    public record Row(Long id, String name, long submissions, long interviewsScheduled, long placements,
            Map<String, ReportLink> links) {
    }
}
