package com.ensar.clmp.history.web;

import java.time.Instant;

import com.ensar.clmp.history.domain.ChangeType;

/** One history row as a viewer sees it; {@code description} is rendered for that viewer. */
public record HistoryEntry(Long id, Instant occurredAt, String actor, ChangeType changeType, String field,
        String oldValue, String newValue, String reason, String note, String description) {
}
