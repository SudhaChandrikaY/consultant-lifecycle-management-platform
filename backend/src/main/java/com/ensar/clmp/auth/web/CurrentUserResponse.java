package com.ensar.clmp.auth.web;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;

public record CurrentUserResponse(Long id, String username, String displayName, Role role, Long recruiterId,
        long sessionTimeoutMinutes) {

    public static CurrentUserResponse from(CurrentUser user, long sessionTimeoutMinutes) {
        return new CurrentUserResponse(user.userId(), user.username(), user.displayName(), user.role(),
                user.recruiterId(), sessionTimeoutMinutes);
    }
}
