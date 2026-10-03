package com.ensar.clmp.recruiter.web;

import jakarta.validation.constraints.NotNull;

import com.ensar.clmp.recruiter.domain.RecruiterStatus;

public record RecruiterStatusRequest(@NotNull(message = "Status is required.") RecruiterStatus status, Boolean confirm,
        @NotNull(message = "Version is required.") Long version) {
}
