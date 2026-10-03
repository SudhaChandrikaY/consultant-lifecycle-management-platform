package com.ensar.clmp.placement.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ensar.clmp.common.domain.GroupCount;

public interface PlacementRepository extends JpaRepository<Placement, Long>, JpaSpecificationExecutor<Placement> {

    @Override
    @EntityGraph(attributePaths = { "consultant", "recruiter", "vendor", "client" })
    Page<Placement> findAll(Specification<Placement> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "consultant", "recruiter", "vendor", "client", "submission" })
    @Query("select p from Placement p where p.id = :id")
    Optional<Placement> findDetailedById(@Param("id") Long id);

    @Query("select p.recruiter.id as groupId, count(p) as count from Placement p "
            + "where p.createdAt >= :from and p.createdAt < :to group by p.recruiter.id")
    List<GroupCount> countByRecruiterCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);

    Optional<Placement> findFirstByConsultant_IdOrderByCreatedAtDescIdDesc(Long consultantId);

    @EntityGraph(attributePaths = { "client", "recruiter" })
    List<Placement> findByConsultant_IdOrderByCreatedAtDesc(Long consultantId);

    @EntityGraph(attributePaths = { "client", "vendor" })
    @Query("select p from Placement p where p.id in :ids")
    List<Placement> findLabelSourcesByIdIn(@Param("ids") Collection<Long> ids);
}
