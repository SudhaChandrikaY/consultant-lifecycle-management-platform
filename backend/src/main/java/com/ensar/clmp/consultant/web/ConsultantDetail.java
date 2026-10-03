package com.ensar.clmp.consultant.web;

import java.time.LocalDate;
import java.util.List;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;
import com.ensar.clmp.reference.domain.VisaType;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Role-shaped consultant details (research R8). Null sections are omitted entirely: {@code contact}
 * for viewers who may not see it (FR-103), and the commercial panels for HR_OPERATIONS (FR-064).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConsultantDetail(
        Long id,
        String firstName,
        String lastName,
        String city,
        String state,
        String primarySkill,
        String additionalSkills,
        Integer yearsExperience,
        VisaType visaType,
        ConsultantStatus status,
        boolean needsReassignment,
        AssignedRecruiter assignedRecruiter,
        Contact contact,
        List<ConsultantStatus> allowedStatusTransitions,
        List<String> missingReadinessItems,
        Long version) {

    public record AssignedRecruiter(Long id, String fullName, RecruiterStatus status) {
    }

    public record Contact(String email, String phone, LocalDate visaExpirationDate, String notes) {
    }
}
