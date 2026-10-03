package com.ensar.clmp.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.domain.AppUser;
import com.ensar.clmp.auth.domain.AppUserRepository;

/** Loads users case-insensitively; inactive users are disabled and cannot sign in (FR-005). */
@Service
public class ClmpUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public ClmpUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsernameIgnoreCase(username == null ? "" : username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password."));
        return new ClmpUserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getDisplayName(), user.getRole(), user.isActive());
    }
}
