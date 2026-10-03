package com.ensar.clmp.history.web;

import java.time.Instant;

import com.ensar.clmp.history.domain.ChangeType;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.domain.TriggerEvent;

/**
 * One history row as a viewer sees it. {@code description} is rendered for that viewer; for
 * HR_OPERATIONS the trigger's entity type and id are null (research R8).
 */
public record HistoryEntry(Long id, Instant occurredAt, String actor, ChangeType changeType, String field,
        String oldValue, String newValue, String reason, String note, boolean systemTriggered, Trigger trigger,
        String description) {

    public record Trigger(TriggerEvent event, HistoryEntityType entityType, Long entityId) {
    }
}
