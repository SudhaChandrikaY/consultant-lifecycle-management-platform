package com.ensar.clmp.history.service;

import java.util.Map;
import java.util.Set;

import com.ensar.clmp.history.domain.HistoryEntityType;

/**
 * Supplies human labels for triggering records (for example "Submission for Acme / Java
 * Developer"). Implemented by the owning module so history never depends on it. Labels may
 * contain commercial details and are never shown to HR_OPERATIONS.
 */
public interface TriggerLabelResolver {

    boolean supports(HistoryEntityType entityType);

    /** One batch lookup for all ids on a page. */
    Map<Long, String> labels(Set<Long> ids);
}
