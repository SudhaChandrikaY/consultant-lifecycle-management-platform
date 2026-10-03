package com.ensar.clmp.consultant.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.ensar.clmp.consultant.domain.ConsultantProfile;
import com.ensar.clmp.reference.domain.VisaType;

/** Create/update body. {@code version} is required on PUT (checked by the service). */
public record ConsultantRequest(
        @NotBlank(message = "First name is required.") @Size(max = 60, message = "First name is too long.") String firstName,
        @NotBlank(message = "Last name is required.") @Size(max = 60, message = "Last name is too long.") String lastName,
        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 254, message = "Email is too long.") String email,
        @Size(max = 30, message = "Phone is too long.") String phone,
        @Size(max = 80, message = "City is too long.") String city,
        @Size(max = 40, message = "State is too long.") String state,
        @Size(max = 80, message = "Primary skill is too long.") String primarySkill,
        @Size(max = 500, message = "Additional skills are too long.") String additionalSkills,
        @Min(value = 0, message = "Years of experience must be between 0 and 50.") @Max(value = 50, message = "Years of experience must be between 0 and 50.") Integer yearsExperience,
        VisaType visaType,
        LocalDate visaExpirationDate,
        @Size(max = 2000, message = "Notes are too long.") String notes,
        Long version) {

    public ConsultantProfile toProfile() {
        return new ConsultantProfile(firstName, lastName, email, phone, city, state, primarySkill, additionalSkills,
                yearsExperience, visaType, visaExpirationDate, notes);
    }

    /** Never print contact values. */
    @Override
    public String toString() {
        return "ConsultantRequest[version=" + version + "]";
    }
}
