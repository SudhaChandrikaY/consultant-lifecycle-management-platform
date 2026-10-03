package com.ensar.clmp.reference.web;

import java.util.List;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.reference.domain.VisaType;

public record ReferenceResponse(List<Item> teams, List<Item> regions, List<VisaType> visaTypes,
        List<ConsultantStatus> consultantStatuses) {

    public record Item(Long id, String code, String name) {
    }
}
