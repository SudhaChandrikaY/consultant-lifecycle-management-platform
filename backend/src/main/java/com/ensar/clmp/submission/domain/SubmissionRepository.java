package com.ensar.clmp.submission.domain;

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

public interface SubmissionRepository extends JpaRepository<Submission, Long>, JpaSpecificationExecutor<Submission> {

    @Override
    @EntityGraph(attributePaths = { "consultant", "recruiter", "vendor", "client" })
    Page<Submission> findAll(Specification<Submission> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "consultant", "recruiter", "vendor", "client" })
    @Query("select s from Submission s where s.id = :id")
    Optional<Submission> findDetailedById(@Param("id") Long id);

    /** Lets callers lock the consultant row before loading the submission graph (research R10). */
    @Query("select s.consultant.id from Submission s where s.id = :id")
    Optional<Long> findConsultantIdById(@Param("id") Long id);

    /** FR-055: same consultant, vendor, client, and normalized job title, in any status. */
    @EntityGraph(attributePaths = "recruiter")
    @Query("select s from Submission s where s.consultant.id = :consultantId and s.vendor.id = :vendorId "
            + "and s.client.id = :clientId and s.jobTitleNormalized = :jobTitle order by s.createdAt, s.id")
    List<Submission> findDuplicates(@Param("consultantId") Long consultantId, @Param("vendorId") Long vendorId,
            @Param("clientId") Long clientId, @Param("jobTitle") String jobTitleNormalized);

    @EntityGraph(attributePaths = { "client", "vendor" })
    @Query("select s from Submission s where s.id in :ids")
    List<Submission> findLabelSourcesByIdIn(@Param("ids") Collection<Long> ids);

    /** Grouped per submission recruiter, for the recruiter performance summary (research R14). */
    @Query("select s.recruiter.id as groupId, count(s) as count from Submission s where s.status in :statuses "
            + "group by s.recruiter.id")
    List<GroupCount> countByRecruiterWithStatusIn(@Param("statuses") Collection<SubmissionStatus> statuses);

    boolean existsByConsultant_IdAndStatusIn(Long consultantId, Collection<SubmissionStatus> statuses);

    List<Submission> findByConsultant_IdAndStatusInOrderById(Long consultantId, Collection<SubmissionStatus> statuses);

    @EntityGraph(attributePaths = { "recruiter", "vendor", "client" })
    List<Submission> findByConsultant_IdOrderByCreatedAtDescIdDesc(Long consultantId);
}
