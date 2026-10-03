package com.ensar.clmp.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A sign-in account. Users are seeded; there is no user administration in the MVP. The password
 * hash is never serialized and is deliberately left out of {@link #toString()} (FR-007).
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stored lower-cased so uniqueness is case-insensitive. */
    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, String displayName, Role role, boolean active) {
        this.username = username.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "AppUser{id=" + id + ", username=" + username + ", role=" + role + "}";
    }
}
