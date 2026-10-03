package com.ensar.clmp.submission.domain;

import java.math.BigDecimal;
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
import com.ensar.clmp.reference.domain.Client;
import com.ensar.clmp.reference.domain.Vendor;
import com.ensar.clmp.reference.service.NameNormalizer;

/**
 * A consultant submitted to a vendor/client job (FR-050). There is no general edit: only status
 * progression and notes (research R19-7). {@code recruiter} keeps reporting credit after the
 * consultant is reassigned.
 */
@Entity
@Table(name = "submission", indexes = {
        @Index(name = "ix_submission_status", columnList = "status"),
        @Index(name = "ix_submission_recruiter", columnList = "recruiter_id"),
        @Index(name = "ix_submission_consultant", columnList = "consultant_id"),
        @Index(name = "ix_submission_submitted", columnList = "submitted_date"),
        @Index(name = "ix_submission_duplicate", columnList = "consultant_id, vendor_id, client_id, job_title_normalized") })
public class Submission implements Versioned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultant_id", nullable = false, updatable = false)
    private Consultant consultant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruiter_id", nullable = false, updatable = false)
    private Recruiter recruiter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false, updatable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @Column(name = "job_title", nullable = false, updatable = false, length = 120)
    private String jobTitle;

    @Column(name = "job_title_normalized", nullable = false, updatable = false, length = 120)
    private String jobTitleNormalized;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "bill_rate", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal billRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status;

    @Column(name = "duplicate_acknowledged", nullable = false, updatable = false)
    private boolean duplicateAcknowledged;

    @Column(name = "duplicate_acknowledged_by_user_id", updatable = false)
    private Long duplicateAcknowledgedByUserId;

    @Column(name = "duplicate_acknowledged_by_name", updatable = false, length = 120)
    private String duplicateAcknowledgedByName;

    @Column(name = "duplicate_acknowledged_at", updatable = false)
    private Instant duplicateAcknowledgedAt;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    protected Submission() {
    }

    public Submission(Consultant consultant, Recruiter recruiter, Vendor vendor, Client client, String jobTitle,
            BigDecimal billRate, SubmissionStatus initialStatus, LocalDate submittedDate, Long createdByUserId,
            Instant now) {
        this.consultant = consultant;
        this.recruiter = recruiter;
        this.vendor = vendor;
        this.client = client;
        this.jobTitle = jobTitle.trim();
        this.jobTitleNormalized = NameNormalizer.normalize(jobTitle);
        this.billRate = billRate;
        this.status = initialStatus;
        this.submittedDate = submittedDate;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
    }

    /** Records an explicit confirmation to proceed despite duplicates (FR-055). */
    public void acknowledgeDuplicate(Long userId, String displayName, Instant at) {
        this.duplicateAcknowledged = true;
        this.duplicateAcknowledgedByUserId = userId;
        this.duplicateAcknowledgedByName = displayName;
        this.duplicateAcknowledgedAt = at;
    }

    public void changeStatus(SubmissionStatus target) {
        this.status = target;
    }

    public void setSubmittedDate(LocalDate submittedDate) {
        this.submittedDate = submittedDate;
    }

    public Long getId() {
        return id;
    }

    public Consultant getConsultant() {
        return consultant;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public Client getClient() {
        return client;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getJobTitleNormalized() {
        return jobTitleNormalized;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    public BigDecimal getBillRate() {
        return billRate;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public boolean isDuplicateAcknowledged() {
        return duplicateAcknowledged;
    }

    public Long getDuplicateAcknowledgedByUserId() {
        return duplicateAcknowledgedByUserId;
    }

    public String getDuplicateAcknowledgedByName() {
        return duplicateAcknowledgedByName;
    }

    public Instant getDuplicateAcknowledgedAt() {
        return duplicateAcknowledgedAt;
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

    @Override
    public String toString() {
        return "Submission{id=" + id + ", status=" + status + "}";
    }
}
