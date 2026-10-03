package com.ensar.clmp.consultant.web;

import jakarta.validation.constraints.NotNull;

public record AssignRecruiterRequest(@NotNull(message = "Recruiter is required.") Long recruiterId,
        @NotNull(message = "Version is required.") Long version) {
}
