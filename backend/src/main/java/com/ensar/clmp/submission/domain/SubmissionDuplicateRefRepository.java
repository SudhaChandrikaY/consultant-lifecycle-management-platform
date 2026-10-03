package com.ensar.clmp.submission.domain;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface SubmissionDuplicateRefRepository extends Repository<SubmissionDuplicateRef, Long> {

    SubmissionDuplicateRef save(SubmissionDuplicateRef ref);

    List<SubmissionDuplicateRef> findBySubmissionId(Long submissionId);
}
