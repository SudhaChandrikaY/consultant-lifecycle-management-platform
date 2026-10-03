package com.ensar.clmp.placement.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.PersonRef;

/** Pre-fill for the placement form (AS 6.1). {@code submittedDate} is the earliest allowed start date. */
public record PlacementDraft(Long submissionId, PersonRef consultant, PersonRef recruiter, NamedRef vendor,
        NamedRef client, String jobTitle, BigDecimal billRate, LocalDate submittedDate) {
}
