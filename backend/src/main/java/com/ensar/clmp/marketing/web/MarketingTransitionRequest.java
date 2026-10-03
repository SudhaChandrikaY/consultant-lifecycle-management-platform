package com.ensar.clmp.marketing.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ensar.clmp.marketing.domain.MarketingStatus;

public record MarketingTransitionRequest(@NotNull(message = "Target status is required.") MarketingStatus targetStatus,
        @Size(max = 500, message = "Reason is too long.") String reason,
        @NotNull(message = "Version is required.") Long version) {
}
