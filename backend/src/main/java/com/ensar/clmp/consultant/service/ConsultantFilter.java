package com.ensar.clmp.consultant.service;

import java.util.List;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.reference.domain.VisaType;

/** Consultant list filter; also used by dashboard counts so figures match lists (research R13). */
public record ConsultantFilter(String q, List<ConsultantStatus> statuses, String primarySkill, VisaType visaType,
        Long recruiterId, Boolean needsReassignment) {

    public static ConsultantFilter none() {
        return new ConsultantFilter(null, null, null, null, null, null);
    }

    public static ConsultantFilter byStatus(ConsultantStatus... statuses) {
        return new ConsultantFilter(null, List.of(statuses), null, null, null, null);
    }
}
