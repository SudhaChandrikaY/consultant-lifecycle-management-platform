package com.ensar.clmp.placement.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.ensar.clmp.common.domain.Versioned;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.reference.domain.Client;
import com.ensar.clmp.reference.domain.Vendor;
import com.ensar.clmp.submission.domain.Submission;

/**
 * A placement created from an Offer submission (FR-070, FR-071). Consultant, recruiter, vendor,
 * client, and job title are copied from the submission; the recruiter is always the submission's.
 * There is no placement status in the MVP.
 */
@Entity
@Table(name = "placement", indexes = {
        @Index(name = "ix_placement_recruiter", columnList = "recruiter_id"),
        @Index(name = "ix_placement_created", columnList = "created_at"),
        @Index(name = "ix_placement_consultant", columnList = "consultant_id") })
public class Placement implements Versioned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, unique = true, updatable = false)
    private Submission submission;

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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "bill_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal billRate;

    @Column(name = "contract_term_months", nullable = false)
    private Integer contractTermMonths;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    @Version
    private Long version;

    protected Placement() {
    }

    public Placement(Submission submission, LocalDate startDate, BigDecimal billRate, int contractTermMonths,
            Long createdByUserId, Instant now) {
        this.submission = submission;
        this.consultant = submission.getConsultant();
        this.recruiter = submission.getRecruiter();
        this.vendor = submission.getVendor();
        this.client = submission.getClient();
        this.jobTitle = submission.getJobTitle();
        this.startDate = startDate;
        this.billRate = billRate;
        this.contractTermMonths = contractTermMonths;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
    }

    /** FR-071. */
    public LocalDate expectedEndDate() {
        return startDate.plusMonths(contractTermMonths);
    }

    public void changeStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void changeBillRate(BigDecimal billRate) {
        this.billRate = billRate;
    }

    public void changeContractTermMonths(Integer contractTermMonths) {
        this.contractTermMonths = contractTermMonths;
    }

    public Long getId() {
        return id;
    }

    public Submission getSubmission() {
        return submission;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public BigDecimal getBillRate() {
        return billRate;
    }

    public Integer getContractTermMonths() {
        return contractTermMonths;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Placement{id=" + id + "}";
    }
}
