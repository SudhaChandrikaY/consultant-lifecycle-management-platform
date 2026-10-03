package com.ensar.clmp.history.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final HistoryDescriptionRenderer renderer;

    public HistoryQueryService(HistoryRecordRepository records, HistoryDescriptionRenderer renderer) {
        this.records = records;
        this.renderer = renderer;
    }

    public Page<HistoryEntry> forEntity(HistoryEntityType entityType, Long entityId, CurrentUser viewer,
            Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        Page<HistoryRecord> page = records.findByEntityTypeAndEntityId(entityType, entityId, sorted);
        return new PageImpl<>(renderer.render(page.getContent(), viewer), sorted, page.getTotalElements());
    }
}
