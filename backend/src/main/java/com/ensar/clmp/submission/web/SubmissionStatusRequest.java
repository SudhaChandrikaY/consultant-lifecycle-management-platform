package com.ensar.clmp.submission.web;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ensar.clmp.submission.domain.SubmissionStatus;

public record SubmissionStatusRequest(@NotNull(message = "Target status is required.") SubmissionStatus targetStatus,
        @Size(max = 2000, message = "Note is too long.") String note,
        LocalDate submittedDate,
        @NotNull(message = "Version is required.") Long version) {
}
