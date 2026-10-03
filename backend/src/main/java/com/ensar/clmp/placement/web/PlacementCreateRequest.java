package com.ensar.clmp.placement.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlacementCreateRequest(
        @NotNull(message = "Submission is required.") Long submissionId,
        @NotNull(message = "Start date is required.") LocalDate startDate,
        @NotNull(message = "Bill rate is required.")
        @DecimalMin(value = "0", inclusive = false, message = "Bill rate must be greater than 0.")
        @Digits(integer = 8, fraction = 2, message = "Bill rate must have at most 2 decimal places.") BigDecimal billRate,
        @NotNull(message = "Contract term is required.")
        @Min(value = 1, message = "Contract term must be 1 to 60 months.")
        @Max(value = 60, message = "Contract term must be 1 to 60 months.") Integer contractTermMonths) {
}
