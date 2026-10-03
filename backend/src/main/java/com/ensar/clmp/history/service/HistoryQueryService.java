package com.ensar.clmp.history.service;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.domain.HistoryRecord;
import com.ensar.clmp.history.domain.HistoryRecordRepository;
import com.ensar.clmp.history.web.HistoryEntry;

/** Reads history for display. Callers check that the viewer may see the entity first. */
@Service
@Transactional(readOnly = true)
public class HistoryQueryService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));

    private final HistoryRecordRepository records;

    public HistoryQueryService(HistoryRecordRepository records) {
        this.records = records;
    }

    public Page<HistoryEntry> forEntity(HistoryEntityType entityType, Long entityId, CurrentUser viewer,
            Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        return records.findByEntityTypeAndEntityId(entityType, entityId, sorted).map(r -> toEntry(r, viewer));
    }

    HistoryEntry toEntry(HistoryRecord r, CurrentUser viewer) {
        return new HistoryEntry(r.getId(), r.getOccurredAt(), r.getActorDisplayName(), r.getChangeType(),
                r.getFieldName(), r.getOldValue(), r.getNewValue(), r.getReason(), r.getNote(), describe(r));
    }

    static String describe(HistoryRecord r) {
        String entity = entityLabel(r.getEntityType());
        return switch (r.getChangeType()) {
            case CREATED -> r.getNewValue() == null ? entity + " created"
                    : entity + " created as " + statusLabel(r.getNewValue());
            case STATUS -> "Status changed from " + statusLabel(r.getOldValue()) + " to " + statusLabel(r.getNewValue());
            case RECRUITER_ASSIGNMENT -> r.getOldValue() == null ? "Recruiter assigned: " + r.getNewValue()
                    : "Recruiter changed from " + r.getOldValue() + " to " + r.getNewValue();
            case OWNER_TRANSFER -> "Owner transferred from " + r.getOldValue() + " to " + r.getNewValue();
            case PROFILE_UPDATED -> "Profile updated (" + r.getFieldName() + ")";
            case FIELD_EDIT -> fieldLabel(r.getFieldName()) + " changed from " + r.getOldValue() + " to "
                    + r.getNewValue();
            case NOTE_ADDED -> "Note added";
        };
    }

    /** BENCH -> Bench, ACTIVE_PROJECT -> Active Project. */
    public static String statusLabel(String code) {
        if (code == null) {
            return "—";
        }
        StringBuilder out = new StringBuilder();
        for (String word : code.toLowerCase(Locale.ROOT).split("_")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    static String entityLabel(HistoryEntityType type) {
        return switch (type) {
            case CONSULTANT -> "Consultant";
            case RECRUITER -> "Recruiter";
            case MARKETING_ASSIGNMENT -> "Marketing assignment";
            case SUBMISSION -> "Submission";
            case PLACEMENT -> "Placement";
        };
    }

    static String fieldLabel(String field) {
        if (field == null) {
            return "Field";
        }
        String spaced = field.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
