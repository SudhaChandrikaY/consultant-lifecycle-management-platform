package com.ensar.clmp.consultant.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.consultant.domain.Consultant;

/** Ownership rules for consultants (research R7, FR-026, FR-103). */
@Component
public class ConsultantAccessPolicy {

    public boolean canView(Consultant consultant, CurrentUser user) {
        return switch (user.role()) {
            case ADMIN, MANAGER, HR_OPERATIONS -> true;
            case RECRUITER -> isAssignedRecruiter(consultant, user);
        };
    }

    public void assertCanView(Consultant consultant, CurrentUser user) {
        if (!canView(consultant, user)) {
            throw new AccessDeniedException("Consultant is outside the caller's scope");
        }
    }

    public void assertCanEditProfile(CurrentUser user) {
        if (!user.isAny(Role.ADMIN, Role.HR_OPERATIONS)) {
            throw new AccessDeniedException("Only ADMIN and HR_OPERATIONS edit consultant profiles");
        }
    }

    public boolean canSeeContact(Consultant consultant, CurrentUser user) {
        return canView(consultant, user);
    }

    /** HR_OPERATIONS never sees marketing, submissions, placements, vendors, clients, or rates (FR-064). */
    public boolean canSeeCommercialSections(CurrentUser user) {
        return user.role() != Role.HR_OPERATIONS;
    }

    public boolean isAssignedRecruiter(Consultant consultant, CurrentUser user) {
        return user.isRecruiter(consultant.getCurrentRecruiterId());
    }
}
