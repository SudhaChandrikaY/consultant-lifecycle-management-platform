package com.ensar.clmp.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Links an acknowledged duplicate to each earlier submission it matched (FR-055). */
@Entity
@Table(name = "submission_duplicate_ref")
public class SubmissionDuplicateRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private Long submissionId;

    @Column(name = "earlier_submission_id", nullable = false, updatable = false)
    private Long earlierSubmissionId;

    protected SubmissionDuplicateRef() {
    }

    public SubmissionDuplicateRef(Long submissionId, Long earlierSubmissionId) {
        this.submissionId = submissionId;
        this.earlierSubmissionId = earlierSubmissionId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public Long getEarlierSubmissionId() {
        return earlierSubmissionId;
    }
}
