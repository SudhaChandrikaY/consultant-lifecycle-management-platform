package com.ensar.clmp.history.service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.domain.HistoryRecord;
import com.ensar.clmp.history.domain.TriggerEvent;
import com.ensar.clmp.history.web.HistoryEntry;

/**
 * Renders history rows for a specific viewer at read time (research R8). Non-HR viewers get
 * trigger descriptions labelled with the triggering record; HR_OPERATIONS gets generic text and
 * no trigger entity type or id, so vendors, clients, and rates never reach them (FR-064, FR-082).
 */
@Component
public class HistoryDescriptionRenderer {

    private final List<TriggerLabelResolver> resolvers;

    public HistoryDescriptionRenderer(List<TriggerLabelResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public List<HistoryEntry> render(List<HistoryRecord> records, CurrentUser viewer) {
        boolean redacted = viewer.role() == Role.HR_OPERATIONS;
        Map<HistoryEntityType, Map<Long, String>> labels = redacted ? Map.of() : resolveLabels(records);
        return records.stream().map(r -> toEntry(r, redacted, labels)).toList();
    }

    private Map<HistoryEntityType, Map<Long, String>> resolveLabels(List<HistoryRecord> records) {
        Map<HistoryEntityType, Set<Long>> ids = new EnumMap<>(HistoryEntityType.class);
        for (HistoryRecord r : records) {
            if (r.isSystemTriggered() && r.getTriggerEntityType() != null) {
                ids.computeIfAbsent(r.getTriggerEntityType(), t -> new HashSet<>()).add(r.getTriggerEntityId());
            }
        }
        Map<HistoryEntityType, Map<Long, String>> out = new EnumMap<>(HistoryEntityType.class);
        ids.forEach((type, set) -> resolvers.stream().filter(res -> res.supports(type)).findFirst()
                .ifPresent(res -> out.put(type, new HashMap<>(res.labels(set)))));
        return out;
    }

    private static HistoryEntry toEntry(HistoryRecord r, boolean redacted,
            Map<HistoryEntityType, Map<Long, String>> labels) {
        HistoryEntry.Trigger trigger = null;
        String description;
        if (r.isSystemTriggered()) {
            trigger = redacted ? new HistoryEntry.Trigger(r.getTriggerEvent(), null, null)
                    : new HistoryEntry.Trigger(r.getTriggerEvent(), r.getTriggerEntityType(), r.getTriggerEntityId());
            String label = labels.getOrDefault(r.getTriggerEntityType(), Map.of()).get(r.getTriggerEntityId());
            description = label == null ? genericTrigger(r.getTriggerEvent()) : labelledTrigger(r.getTriggerEvent(), label);
        } else {
            description = describe(r);
        }
        return new HistoryEntry(r.getId(), r.getOccurredAt(), r.getActorDisplayName(), r.getChangeType(),
                r.getFieldName(), r.getOldValue(), r.getNewValue(), r.getReason(), r.getNote(), r.isSystemTriggered(),
                trigger, description);
    }

    /** Generic description of a trigger; never mentions vendors, clients, or rates. */
    static String genericTrigger(TriggerEvent event) {
        return switch (event) {
            case MARKETING_ACTIVATED -> "Marketing activated";
            case MARKETING_REOPENED -> "Marketing reopened";
            case MARKETING_CLOSED -> "Marketing closed";
            case SUBMISSION_INTERVIEW_SCHEDULED -> "Submission moved to Interview Scheduled";
            case SUBMISSION_LEFT_INTERVIEW_STAGES -> "No submissions remain in interview or offer stages";
            case PLACEMENT_CREATED -> "Placement created";
            case CONSULTANT_HOLD -> "Consultant put on Hold";
            case CONSULTANT_INACTIVE -> "Consultant set to Inactive";
        };
    }

    static String labelledTrigger(TriggerEvent event, String label) {
        return switch (event) {
            case SUBMISSION_INTERVIEW_SCHEDULED -> label + " moved to Interview Scheduled";
            case SUBMISSION_LEFT_INTERVIEW_STAGES -> label + " left interview stages; none remain in interview or offer";
            case PLACEMENT_CREATED -> label;
            default -> genericTrigger(event);
        };
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
