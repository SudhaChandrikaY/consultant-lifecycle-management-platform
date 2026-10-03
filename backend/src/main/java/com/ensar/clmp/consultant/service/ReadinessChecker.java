package com.ensar.clmp.consultant.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;

/** FR-035: what a consultant still needs before being marked Ready, in display order. */
@Component
public class ReadinessChecker {

    public List<String> missingItems(Consultant c) {
        List<String> missing = new ArrayList<>();
        if (blank(c.getFirstName())) {
            missing.add("firstName");
        }
        if (blank(c.getLastName())) {
            missing.add("lastName");
        }
        if (blank(c.getEmail())) {
            missing.add("email");
        }
        if (blank(c.getPhone())) {
            missing.add("phone");
        }
        if (blank(c.getPrimarySkill())) {
            missing.add("primarySkill");
        }
        if (c.getYearsExperience() == null) {
            missing.add("yearsExperience");
        }
        if (c.getVisaType() == null) {
            missing.add("visaType");
        }
        if (c.getCurrentRecruiter() == null || c.getCurrentRecruiter().getStatus() != RecruiterStatus.ACTIVE) {
            missing.add("assignedActiveRecruiter");
        }
        return missing;
    }

    public boolean isReady(Consultant c) {
        return missingItems(c).isEmpty();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
