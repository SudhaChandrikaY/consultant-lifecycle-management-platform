package com.ensar.clmp.history.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/**
 * Append-only by construction (FR-101): extends {@link Repository}, not JpaRepository, so there
 * is no update or delete method.
 */
public interface HistoryRecordRepository
        extends Repository<HistoryRecord, Long>, JpaSpecificationExecutor<HistoryRecord> {

    HistoryRecord save(HistoryRecord record);

    Page<HistoryRecord> findByEntityTypeAndEntityId(HistoryEntityType entityType, Long entityId, Pageable pageable);

    long count();
}
