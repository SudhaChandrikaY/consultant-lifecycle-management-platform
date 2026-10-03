package com.ensar.clmp.marketing.web;

import java.time.LocalDate;

import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.PersonRef;
import com.ensar.clmp.marketing.domain.MarketingAssignment;
import com.ensar.clmp.marketing.domain.MarketingStatus;

public record MarketingListItem(Long id, PersonRef consultant, PersonRef ownerRecruiter, NamedRef team,
        LocalDate startDate, LocalDate targetDate, MarketingStatus status, boolean overdue) {

    public static MarketingListItem from(MarketingAssignment a, LocalDate today) {
        return new MarketingListItem(a.getId(),
                new PersonRef(a.getConsultant().getId(), a.getConsultant().getFullName()),
                new PersonRef(a.getOwnerRecruiter().getId(), a.getOwnerRecruiter().getFullName()),
                new NamedRef(a.getOwnerTeam().getId(), a.getOwnerTeam().getName()),
                a.getStartDate(), a.getTargetDate(), a.getStatus(), a.isOverdue(today));
    }
}
