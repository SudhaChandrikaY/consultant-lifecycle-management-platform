package com.ensar.clmp.consultant.domain;

import java.time.LocalDate;

import com.ensar.clmp.reference.domain.VisaType;

/** The editable profile fields of a consultant (FR-020), in display order. */
public record ConsultantProfile(String firstName, String lastName, String email, String phone, String city,
        String state, String primarySkill, String additionalSkills, Integer yearsExperience, VisaType visaType,
        LocalDate visaExpirationDate, String notes) {

    /** Trims text, turns blanks into null, and lower-cases the email. */
    public ConsultantProfile normalized() {
        return new ConsultantProfile(trim(firstName), trim(lastName),
                email == null ? null : email.trim().toLowerCase(), trim(phone), trim(city), trim(state),
                trim(primarySkill), trim(additionalSkills), yearsExperience, visaType, visaExpirationDate, trim(notes));
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
