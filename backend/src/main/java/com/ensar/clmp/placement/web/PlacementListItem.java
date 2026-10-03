package com.ensar.clmp.placement.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.PersonRef;
import com.ensar.clmp.placement.domain.Placement;

public record PlacementListItem(Long id, PersonRef consultant, PersonRef recruiter, NamedRef client, NamedRef vendor,
        LocalDate startDate, BigDecimal billRate, Integer contractTermMonths, LocalDate expectedEndDate) {

    public static PlacementListItem from(Placement p) {
        return new PlacementListItem(p.getId(), new PersonRef(p.getConsultant().getId(), p.getConsultant().getFullName()),
                new PersonRef(p.getRecruiter().getId(), p.getRecruiter().getFullName()),
                new NamedRef(p.getClient().getId(), p.getClient().getName()),
                new NamedRef(p.getVendor().getId(), p.getVendor().getName()), p.getStartDate(), p.getBillRate(),
                p.getContractTermMonths(), p.expectedEndDate());
    }
}
