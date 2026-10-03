package com.ensar.clmp.submission.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.NoteResponse;
import com.ensar.clmp.common.web.PersonRef;
import com.ensar.clmp.history.web.HistoryEntry;
import com.ensar.clmp.submission.domain.SubmissionStatus;

public record SubmissionDetail(Long id, PersonRef consultant, PersonRef recruiter, NamedRef vendor, NamedRef client,
        String jobTitle, LocalDate submittedDate, BigDecimal billRate, SubmissionStatus status,
        DuplicateAcknowledgement duplicateAcknowledgement, List<NoteResponse> notes, List<HistoryEntry> timeline,
        List<SubmissionStatus> allowedTransitions, boolean canCreatePlacement, Long version) {

    public record DuplicateAcknowledgement(String acknowledgedBy, Instant acknowledgedAt,
            List<Long> earlierSubmissionIds) {
    }
}
