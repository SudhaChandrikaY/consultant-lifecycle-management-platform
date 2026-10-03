package com.ensar.clmp.placement.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** ADMIN edit (FR-074); absent fields are left unchanged. */
public record PlacementPatchRequest(
        LocalDate startDate,
        @DecimalMin(value = "0", inclusive = false, message = "Bill rate must be greater than 0.")
        @Digits(integer = 8, fraction = 2, message = "Bill rate must have at most 2 decimal places.") BigDecimal billRate,
        @Min(value = 1, message = "Contract term must be 1 to 60 months.")
        @Max(value = 60, message = "Contract term must be 1 to 60 months.") Integer contractTermMonths,
        @NotNull(message = "Version is required.") Long version) {
}
