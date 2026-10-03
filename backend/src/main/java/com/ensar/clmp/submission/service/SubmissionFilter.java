package com.ensar.clmp.submission.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.ensar.clmp.submission.domain.SubmissionStatus;

/** Submission list filter; also used for dashboard and report counts (research R13). */
public record SubmissionFilter(List<SubmissionStatus> statuses, Long recruiterId, Long vendorId, Long clientId,
        Long consultantId, LocalDate submittedFrom, LocalDate submittedTo) {

    public static SubmissionFilter byStatuses(Collection<SubmissionStatus> statuses) {
        return new SubmissionFilter(List.copyOf(statuses), null, null, null, null, null, null);
    }
}
