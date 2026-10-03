package com.ensar.clmp.submission.web;

import java.time.Instant;
import java.time.LocalDate;

import com.ensar.clmp.submission.domain.SubmissionStatus;

/** An earlier matching submission listed in the FR-055 duplicate warning. */
public record DuplicateSummary(Long id, SubmissionStatus status, LocalDate submittedDate, Instant createdAt,
        String recruiterName) {
}
