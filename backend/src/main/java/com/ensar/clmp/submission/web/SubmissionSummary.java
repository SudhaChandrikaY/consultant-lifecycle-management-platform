package com.ensar.clmp.submission.web;

import java.time.LocalDate;

import com.ensar.clmp.submission.domain.Submission;
import com.ensar.clmp.submission.domain.SubmissionStatus;

/** A row in the consultant detail page's Submissions panel (never sent to HR_OPERATIONS). */
public record SubmissionSummary(Long id, String vendorName, String clientName, String jobTitle,
        SubmissionStatus status, LocalDate submittedDate) {

    public static SubmissionSummary from(Submission s) {
        return new SubmissionSummary(s.getId(), s.getVendor().getName(), s.getClient().getName(), s.getJobTitle(),
                s.getStatus(), s.getSubmittedDate());
    }
}
