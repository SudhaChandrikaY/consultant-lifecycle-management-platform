package com.ensar.clmp.submission.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Append-only submission note (FR-060). */
@Entity
@Table(name = "submission_note", indexes = @Index(name = "ix_submission_note", columnList = "submission_id, created_at"))
public class SubmissionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private Long submissionId;

    @Column(nullable = false, updatable = false, length = 2000)
    private String body;

    @Column(name = "author_user_id", nullable = false, updatable = false)
    private Long authorUserId;

    @Column(name = "author_display_name", nullable = false, updatable = false, length = 120)
    private String authorDisplayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SubmissionNote() {
    }

    public SubmissionNote(Long submissionId, String body, Long authorUserId, String authorDisplayName,
            Instant createdAt) {
        this.submissionId = submissionId;
        this.body = body;
        this.authorUserId = authorUserId;
        this.authorDisplayName = authorDisplayName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getBody() {
        return body;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
