package com.ensar.clmp.consultant.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ensar.clmp.consultant.domain.ConsultantStatus;

public record StatusChangeRequest(
        @NotNull(message = "Target status is required.") ConsultantStatus targetStatus,
        @Size(max = 500, message = "Reason is too long.") String reason,
        @NotNull(message = "Version is required.") Long version) {
}
