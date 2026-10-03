package com.ensar.clmp.consultant.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;
import com.ensar.clmp.reference.domain.VisaType;

/**
 * A consultant profile and its lifecycle status. Deliberately has no SSN, date of birth, bank,
 * or identity-document fields (FR-027), and is never deleted (FR-028). Status changes go through
 * ConsultantLifecycleService only.
 */
@Entity
@Table(name = "consultant", indexes = {
        @Index(name = "ix_consultant_status", columnList = "status"),
        @Index(name = "ix_consultant_recruiter", columnList = "current_recruiter_id") })
public class Consultant implements Versioned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    /** Stored lower-cased; unique case-insensitively (AS 2.5). */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 80)
    private String city;

    @Column(length = 40)
    private String state;

    @Column(name = "primary_skill", length = 80)
    private String primarySkill;

    @Column(name = "additional_skills", length = 500)
    private String additionalSkills;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "visa_type", length = 20)
    private VisaType visaType;

    @Column(name = "visa_expiration_date")
    private LocalDate visaExpirationDate;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultantStatus status = ConsultantStatus.BENCH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_recruiter_id")
    private Recruiter currentRecruiter;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Consultant() {
    }

    public Consultant(ConsultantProfile profile, Long createdByUserId, Instant now) {
        apply(profile.normalized());
        this.status = ConsultantStatus.BENCH;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Applies a profile edit and returns the names of the fields that changed (for history). */
    public List<String> updateProfile(ConsultantProfile profile, Instant now) {
        ConsultantProfile next = profile.normalized();
        ConsultantProfile current = profile();
        List<String> changed = new ArrayList<>();
        diff(changed, "firstName", current.firstName(), next.firstName());
        diff(changed, "lastName", current.lastName(), next.lastName());
        diff(changed, "email", current.email(), next.email());
        diff(changed, "phone", current.phone(), next.phone());
        diff(changed, "city", current.city(), next.city());
        diff(changed, "state", current.state(), next.state());
        diff(changed, "primarySkill", current.primarySkill(), next.primarySkill());
        diff(changed, "additionalSkills", current.additionalSkills(), next.additionalSkills());
        diff(changed, "yearsExperience", current.yearsExperience(), next.yearsExperience());
        diff(changed, "visaType", current.visaType(), next.visaType());
        diff(changed, "visaExpirationDate", current.visaExpirationDate(), next.visaExpirationDate());
        diff(changed, "notes", current.notes(), next.notes());
        if (!changed.isEmpty()) {
            apply(next);
            this.updatedAt = now;
        }
        return changed;
    }

    /** Package-private on purpose in spirit: only the lifecycle service changes status. */
    public void changeStatus(ConsultantStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    /** Replaces the single primary recruiter (FR-033). */
    public void assignRecruiter(Recruiter recruiter, Instant now) {
        this.currentRecruiter = recruiter;
        this.updatedAt = now;
    }

    public ConsultantProfile profile() {
        return new ConsultantProfile(firstName, lastName, email, phone, city, state, primarySkill, additionalSkills,
                yearsExperience, visaType, visaExpirationDate, notes);
    }

    /** Derived: assigned to a recruiter who is now inactive (research R18-3). */
    public boolean needsReassignment() {
        return currentRecruiter != null && currentRecruiter.getStatus() == RecruiterStatus.INACTIVE;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    private void apply(ConsultantProfile p) {
        this.firstName = p.firstName();
        this.lastName = p.lastName();
        this.email = p.email();
        this.phone = p.phone();
        this.city = p.city();
        this.state = p.state();
        this.primarySkill = p.primarySkill();
        this.additionalSkills = p.additionalSkills();
        this.yearsExperience = p.yearsExperience();
        this.visaType = p.visaType();
        this.visaExpirationDate = p.visaExpirationDate();
        this.notes = p.notes();
    }

    private static void diff(List<String> changed, String name, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changed.add(name);
        }
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPrimarySkill() {
        return primarySkill;
    }

    public String getAdditionalSkills() {
        return additionalSkills;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public VisaType getVisaType() {
        return visaType;
    }

    public LocalDate getVisaExpirationDate() {
        return visaExpirationDate;
    }

    public String getNotes() {
        return notes;
    }

    public ConsultantStatus getStatus() {
        return status;
    }

    public Recruiter getCurrentRecruiter() {
        return currentRecruiter;
    }

    public Long getCurrentRecruiterId() {
        return currentRecruiter == null ? null : currentRecruiter.getId();
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        // Ids only: never log contact details (constitution VII).
        return "Consultant{id=" + id + ", status=" + status + "}";
    }
}
