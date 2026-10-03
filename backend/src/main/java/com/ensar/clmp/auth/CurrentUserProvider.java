package com.ensar.clmp.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.service.ClmpUserPrincipal;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;

/** Resolves the caller from the security context, including the linked recruiter profile. */
@Component
public class CurrentUserProvider {

    private final RecruiterRepository recruiters;

    public CurrentUserProvider(RecruiterRepository recruiters) {
        this.recruiters = recruiters;
    }

    @Transactional(readOnly = true)
    public CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ClmpUserPrincipal principal)) {
            throw new AccessDeniedException("No authenticated user");
        }
        Long recruiterId = recruiters.findByUserId(principal.getUserId()).map(Recruiter::getId).orElse(null);
        return new CurrentUser(principal.getUserId(), principal.getUsername(), principal.getDisplayName(),
                principal.getRole(), recruiterId);
    }
}
