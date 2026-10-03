package com.ensar.clmp.recruiter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.ensar.clmp.auth.domain.AppUser;
import com.ensar.clmp.common.domain.Versioned;
import com.ensar.clmp.reference.domain.Region;
import com.ensar.clmp.reference.domain.Team;

/** A recruiter profile, optionally linked to one RECRUITER sign-in account (FR-015). */
@Entity
@Table(name = "recruiter")
public class Recruiter implements Versioned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    /** Stored lower-cased so uniqueness is case-insensitive (FR-010). */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruiterStatus status = RecruiterStatus.ACTIVE;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private AppUser user;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Recruiter() {
    }

    public Recruiter(String fullName, String email, String phone, Team team, Region region, AppUser user,
            Instant now) {
        this.fullName = fullName;
        this.email = email.trim().toLowerCase();
        this.phone = phone;
        this.team = team;
        this.region = region;
        this.user = user;
        this.status = RecruiterStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String fullName, String email, String phone, Team team, Region region, AppUser user,
            Instant now) {
        this.fullName = fullName;
        this.email = email.trim().toLowerCase();
        this.phone = phone;
        this.team = team;
        this.region = region;
        this.user = user;
        this.updatedAt = now;
    }

    public void changeStatus(RecruiterStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == RecruiterStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Team getTeam() {
        return team;
    }

    public Region getRegion() {
        return region;
    }

    public RecruiterStatus getStatus() {
        return status;
    }

    public AppUser getUser() {
        return user;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
