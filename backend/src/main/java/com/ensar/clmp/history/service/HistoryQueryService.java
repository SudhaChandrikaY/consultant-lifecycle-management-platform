package com.ensar.clmp.history.service;

import java.util.List;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.consultant.domain.Consultant;
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

    /**
     * The viewer's most recent activity (FR-102): ADMIN and MANAGER see everything; a RECRUITER sees
     * rows about consultants currently assigned to them or records they own; HR_OPERATIONS sees
     * consultant rows only, rendered without commercial details.
     */
    public List<HistoryEntry> recentActivity(CurrentUser viewer, int limit) {
        if (viewer.role() == Role.RECRUITER && viewer.recruiterId() == null) {
            return List.of();
        }
        Page<HistoryRecord> page = records.findAll(activityScope(viewer), PageRequest.of(0, limit, NEWEST_FIRST));
        return renderer.render(page.getContent(), viewer);
    }

    private static Specification<HistoryRecord> activityScope(CurrentUser viewer) {
        return (root, query, cb) -> switch (viewer.role()) {
            case ADMIN, MANAGER -> cb.conjunction();
            case HR_OPERATIONS -> cb.equal(root.get("entityType"), HistoryEntityType.CONSULTANT);
            case RECRUITER -> {
                Subquery<Long> mine = query.subquery(Long.class);
                Root<Consultant> c = mine.from(Consultant.class);
                mine.select(c.get("id")).where(cb.equal(c.get("currentRecruiter").get("id"), viewer.recruiterId()));
                yield cb.or(root.get("consultantId").in(mine),
                        cb.equal(root.get("ownerRecruiterId"), viewer.recruiterId()));
            }
        };
    }

    public Page<HistoryEntry> forEntity(HistoryEntityType entityType, Long entityId, CurrentUser viewer,
            Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        Page<HistoryRecord> page = records.findByEntityTypeAndEntityId(entityType, entityId, sorted);
        return new PageImpl<>(renderer.render(page.getContent(), viewer), sorted, page.getTotalElements());
    }
}
