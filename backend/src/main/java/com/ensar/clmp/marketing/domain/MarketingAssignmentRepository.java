package com.ensar.clmp.marketing.domain;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MarketingAssignmentRepository
        extends JpaRepository<MarketingAssignment, Long>, JpaSpecificationExecutor<MarketingAssignment> {

    @Override
    @EntityGraph(attributePaths = { "consultant", "ownerRecruiter", "ownerTeam" })
    Page<MarketingAssignment> findAll(Specification<MarketingAssignment> spec, Pageable pageable);

    /** The consultant's open (Draft/Active/Hold) assignment, if any (FR-042). */
    Optional<MarketingAssignment> findFirstByConsultant_IdAndStatusIn(Long consultantId,
            Collection<MarketingStatus> statuses);

    boolean existsByConsultant_IdAndStatus(Long consultantId, MarketingStatus status);

    default Optional<MarketingAssignment> findOpenFor(Long consultantId) {
        return findFirstByConsultant_IdAndStatusIn(consultantId, MarketingStatus.OPEN);
    }
}
