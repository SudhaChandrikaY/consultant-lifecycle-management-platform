package com.ensar.clmp.marketing.web;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record MarketingCreateRequest(@NotNull(message = "Consultant is required.") Long consultantId,
        Long ownerRecruiterId,
        @NotNull(message = "Start date is required.") LocalDate startDate,
        @NotNull(message = "Target date is required.") LocalDate targetDate) {
}
