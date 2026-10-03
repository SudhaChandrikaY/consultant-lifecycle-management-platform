package com.ensar.clmp.marketing.web;

import java.time.LocalDate;
import java.util.List;

import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.NoteResponse;
import com.ensar.clmp.common.web.PersonRef;
import com.ensar.clmp.marketing.domain.MarketingStatus;

public record MarketingDetail(Long id, PersonRef consultant, PersonRef ownerRecruiter, NamedRef team,
        LocalDate startDate, LocalDate targetDate, MarketingStatus status, boolean overdue, String holdReason,
        String closeReason, List<NoteResponse> notes, List<MarketingStatus> allowedTransitions, Long version) {
}
