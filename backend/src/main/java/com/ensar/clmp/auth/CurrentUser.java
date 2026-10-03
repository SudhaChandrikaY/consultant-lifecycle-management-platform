package com.ensar.clmp.auth;

import com.ensar.clmp.auth.domain.Role;

/**
 * The signed-in caller as services see it. {@code recruiterId} is the linked recruiter profile,
 * or null when the user is not linked (that user gets an empty scope).
 */
public record CurrentUser(Long userId, String username, String displayName, Role role, Long recruiterId) {

    public boolean is(Role candidate) {
        return role == candidate;
    }

    public boolean isAny(Role... candidates) {
        for (Role candidate : candidates) {
            if (role == candidate) {
                return true;
            }
        }
        return false;
    }

    /** True for a RECRUITER whose linked recruiter profile is {@code recruiterIdToCheck}. */
    public boolean isRecruiter(Long recruiterIdToCheck) {
        return role == Role.RECRUITER && recruiterId != null && recruiterId.equals(recruiterIdToCheck);
    }
}
