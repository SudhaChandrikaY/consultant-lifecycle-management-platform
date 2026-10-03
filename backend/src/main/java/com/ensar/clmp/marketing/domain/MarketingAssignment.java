package com.ensar.clmp.marketing.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.ensar.clmp.common.domain.Versioned;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.reference.domain.Team;

/** A marketing effort for one consultant (FR-040–FR-045). Status changes go through MarketingService. */
@Entity
@Table(name = "marketing_assignment", indexes = {
        @Index(name = "ix_marketing_consultant_status", columnList = "consultant_id, status"),
        @Index(name = "ix_marketing_owner", columnList = "owner_recruiter_id") })
public class MarketingAssignment implements Versioned {

    /** System close reason when a placement is created (FR-032); such an assignment can't be reopened. */
    public static final String PLACED_REASON = "Placed";
    /** System close reason when the consultant is set to Inactive (FR-032). */
    public static final String INACTIVE_REASON = "Consultant inactive";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultant_id", nullable = false, updatable = false)
    private Consultant consultant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_recruiter_id", nullable = false)
    private Recruiter ownerRecruiter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_team_id", nullable = false)
    private Team ownerTeam;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketingStatus status = MarketingStatus.DRAFT;

    @Column(name = "hold_reason", length = 500)
    private String holdReason;

    @Column(name = "close_reason", length = 500)
    private String closeReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    protected MarketingAssignment() {
    }

    public MarketingAssignment(Consultant consultant, Recruiter owner, LocalDate startDate, LocalDate targetDate,
            Long createdByUserId, Instant now) {
        this.consultant = consultant;
        transferTo(owner);
        this.startDate = startDate;
        this.targetDate = targetDate;
        this.status = MarketingStatus.DRAFT;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
    }

    /** Owner recruiter and team move together (FR-040, reassignment edge case). */
    public void transferTo(Recruiter owner) {
        this.ownerRecruiter = owner;
        this.ownerTeam = owner.getTeam();
    }

    public void changeDates(LocalDate startDate, LocalDate targetDate) {
        this.startDate = startDate;
        this.targetDate = targetDate;
    }

    /** Applies a status change; reasons are kept for Hold and Closed and cleared on reopen. */
    public void changeStatus(MarketingStatus target, String reason, Instant now) {
        switch (target) {
            case HOLD -> this.holdReason = reason;
            case CLOSED -> {
                this.closeReason = reason;
                this.closedAt = now;
            }
            case ACTIVE -> {
                this.holdReason = null;
                this.closeReason = null;
                this.closedAt = null;
            }
            case DRAFT -> {
            }
        }
        this.status = target;
    }

    /** FR-043: past the target date and not Closed. */
    public boolean isOverdue(LocalDate today) {
        return status != MarketingStatus.CLOSED && targetDate.isBefore(today);
    }

    public boolean isOpen() {
        return MarketingStatus.OPEN.contains(status);
    }

    public Long getId() {
        return id;
    }

    public Consultant getConsultant() {
        return consultant;
    }

    public Recruiter getOwnerRecruiter() {
        return ownerRecruiter;
    }

    public Team getOwnerTeam() {
        return ownerTeam;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public MarketingStatus getStatus() {
        return status;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }
}
