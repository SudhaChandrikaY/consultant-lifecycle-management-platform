package com.ensar.clmp.auth.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ensar.clmp.auth.domain.Role;

/** Session principal. The password hash is erased after authentication. */
public final class ClmpUserPrincipal implements UserDetails, CredentialsContainer {

    private final Long userId;
    private final String username;
    private String passwordHash;
    private final String displayName;
    private final Role role;
    private final boolean enabled;

    public ClmpUserPrincipal(Long userId, String username, String passwordHash, String displayName, Role role,
            boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    @Override
    public String toString() {
        return "ClmpUserPrincipal{userId=" + userId + ", role=" + role + "}";
    }
}
