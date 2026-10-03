package com.ensar.clmp.marketing.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Append-only marketing note (FR-045): no setters, and the repository has no update or delete. */
@Entity
@Table(name = "marketing_note", indexes = @Index(name = "ix_marketing_note_assignment",
        columnList = "marketing_assignment_id, created_at"))
public class MarketingNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "marketing_assignment_id", nullable = false, updatable = false)
    private Long marketingAssignmentId;

    @Column(nullable = false, updatable = false, length = 2000)
    private String body;

    @Column(name = "author_user_id", nullable = false, updatable = false)
    private Long authorUserId;

    @Column(name = "author_display_name", nullable = false, updatable = false, length = 120)
    private String authorDisplayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MarketingNote() {
    }

    public MarketingNote(Long marketingAssignmentId, String body, Long authorUserId, String authorDisplayName,
            Instant createdAt) {
        this.marketingAssignmentId = marketingAssignmentId;
        this.body = body;
        this.authorUserId = authorUserId;
        this.authorDisplayName = authorDisplayName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMarketingAssignmentId() {
        return marketingAssignmentId;
    }

    public String getBody() {
        return body;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
