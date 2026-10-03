package com.ensar.clmp.consultant.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultantRepository extends JpaRepository<Consultant, Long>, JpaSpecificationExecutor<Consultant> {

    /** List queries fetch the recruiter in the same select to avoid N+1 (research R14). */
    @Override
    @EntityGraph(attributePaths = "currentRecruiter")
    Page<Consultant> findAll(Specification<Consultant> spec, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** Serializes per-consultant invariant checks (research R10). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consultant c where c.id = :id")
    Optional<Consultant> findByIdForUpdate(@Param("id") Long id);

    @Query("select c.status as status, count(c) as count from Consultant c group by c.status")
    List<StatusCount> countByStatus();

    @Query("select distinct c.primarySkill from Consultant c where c.primarySkill is not null order by c.primarySkill")
    List<String> findDistinctPrimarySkills();

    @Query("select c.currentRecruiter.id as recruiterId, count(c) as count from Consultant c "
            + "where c.currentRecruiter.id in :recruiterIds group by c.currentRecruiter.id")
    List<RecruiterCount> countByCurrentRecruiterIdIn(@Param("recruiterIds") Collection<Long> recruiterIds);

    long countByCurrentRecruiterId(Long recruiterId);

    interface StatusCount {
        ConsultantStatus getStatus();

        long getCount();
    }

    interface RecruiterCount {
        Long getRecruiterId();

        long getCount();
    }
}
