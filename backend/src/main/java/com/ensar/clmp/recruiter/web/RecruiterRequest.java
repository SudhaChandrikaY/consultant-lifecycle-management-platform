package com.ensar.clmp.recruiter.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecruiterRequest(
        @NotBlank(message = "Full name is required.") @Size(max = 120, message = "Full name is too long.") String fullName,
        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 254, message = "Email is too long.") String email,
        @Size(max = 30, message = "Phone is too long.") String phone,
        @NotNull(message = "Team is required.") Long teamId,
        @NotNull(message = "Region is required.") Long regionId,
        Long linkedUserId,
        Long version) {

    /** Never print contact values. */
    @Override
    public String toString() {
        return "RecruiterRequest[teamId=" + teamId + ", regionId=" + regionId + ", version=" + version + "]";
    }
}
