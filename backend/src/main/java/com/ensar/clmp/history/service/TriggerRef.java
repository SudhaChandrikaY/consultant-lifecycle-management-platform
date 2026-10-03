package com.ensar.clmp.history.service;

import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.domain.TriggerEvent;

/** Identifies the event and record that caused an automatic change. */
public record TriggerRef(TriggerEvent event, HistoryEntityType entityType, Long entityId) {
}
