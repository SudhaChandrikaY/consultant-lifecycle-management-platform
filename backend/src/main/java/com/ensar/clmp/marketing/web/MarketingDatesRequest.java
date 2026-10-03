package com.ensar.clmp.marketing.web;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record MarketingDatesRequest(@NotNull(message = "Start date is required.") LocalDate startDate,
        @NotNull(message = "Target date is required.") LocalDate targetDate,
        @NotNull(message = "Version is required.") Long version) {
}
