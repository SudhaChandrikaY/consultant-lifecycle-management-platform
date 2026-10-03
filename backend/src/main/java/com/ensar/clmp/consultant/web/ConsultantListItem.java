package com.ensar.clmp.consultant.web;

import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.reference.domain.VisaType;

/** List row. Contains no contact fields, by design (FR-024). */
public record ConsultantListItem(Long id, String fullName, String primarySkill, Integer yearsExperience,
        VisaType visaType, RecruiterRef assignedRecruiter, ConsultantStatus status, boolean needsReassignment) {

    public record RecruiterRef(Long id, String fullName) {
    }

    public static ConsultantListItem from(Consultant c) {
        RecruiterRef recruiter = c.getCurrentRecruiter() == null ? null
                : new RecruiterRef(c.getCurrentRecruiter().getId(), c.getCurrentRecruiter().getFullName());
        return new ConsultantListItem(c.getId(), c.getFullName(), c.getPrimarySkill(), c.getYearsExperience(),
                c.getVisaType(), recruiter, c.getStatus(), c.needsReassignment());
    }
}
