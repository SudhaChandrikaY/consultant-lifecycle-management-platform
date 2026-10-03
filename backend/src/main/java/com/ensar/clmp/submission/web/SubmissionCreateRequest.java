package com.ensar.clmp.submission.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Exactly one of vendorId/vendorName and of clientId/clientName (checked by the service). */
public record SubmissionCreateRequest(
        @NotNull(message = "Consultant is required.") Long consultantId,
        Long recruiterId,
        Long vendorId,
        @Size(max = 120, message = "Vendor name is too long.") String vendorName,
        Long clientId,
        @Size(max = 120, message = "Client name is too long.") String clientName,
        @NotBlank(message = "Job title is required.") @Size(max = 120, message = "Job title is too long.") String jobTitle,
        @NotNull(message = "Bill rate is required.")
        @DecimalMin(value = "0", inclusive = false, message = "Bill rate must be greater than 0.")
        @Digits(integer = 8, fraction = 2, message = "Bill rate must have at most 2 decimal places.") BigDecimal billRate,
        Boolean submitNow,
        LocalDate submittedDate,
        @Size(max = 2000, message = "Note is too long.") String note,
        Boolean acknowledgeDuplicate) {
}
