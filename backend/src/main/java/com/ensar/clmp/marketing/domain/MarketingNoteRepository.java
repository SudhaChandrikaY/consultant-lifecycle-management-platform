package com.ensar.clmp.marketing.domain;

import java.util.List;

import org.springframework.data.repository.Repository;

/** Append-only (FR-045): save and read only. */
public interface MarketingNoteRepository extends Repository<MarketingNote, Long> {

    MarketingNote save(MarketingNote note);

    List<MarketingNote> findByMarketingAssignmentIdOrderByCreatedAtAscIdAsc(Long marketingAssignmentId);
}
