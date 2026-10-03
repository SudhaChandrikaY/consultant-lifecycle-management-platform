package com.ensar.clmp.reference.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.ensar.clmp.reference.service.NameNormalizer;

/** Created implicitly by name during submission entry (FR-051); matched on {@code normalized_name}. */
@Entity
@Table(name = "vendor")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 120)
    private String normalizedName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    protected Vendor() {
    }

    public Vendor(String name, Long createdByUserId, Instant now) {
        this.name = NameNormalizer.display(name);
        this.normalizedName = NameNormalizer.normalize(name);
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
