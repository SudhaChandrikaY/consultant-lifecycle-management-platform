package com.ensar.clmp.placement.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.PersonRef;
import com.ensar.clmp.history.web.HistoryEntry;

public record PlacementDetail(Long id, PersonRef consultant, PersonRef recruiter, NamedRef client, NamedRef vendor,
        LocalDate startDate, BigDecimal billRate, Integer contractTermMonths, LocalDate expectedEndDate,
        Long submissionId, String jobTitle, Instant createdAt, String createdBy, List<HistoryEntry> history,
        Long version) {
}
