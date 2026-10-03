package com.ensar.clmp.report.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.ensar.clmp.submission.domain.SubmissionStatus;

/** Submissions per recruiter by submitted date in range, broken down by current status (AS 8.1). */
public record SubmissionsByRecruiterReport(LocalDate from, LocalDate to, List<Row> rows, Totals totals, boolean empty) {

    public record Row(Long recruiterId, String recruiterName, long total, Map<SubmissionStatus, Long> byStatus,
            Map<String, ReportLink> links) {
    }

    public record Totals(long total, Map<SubmissionStatus, Long> byStatus) {
    }
}
