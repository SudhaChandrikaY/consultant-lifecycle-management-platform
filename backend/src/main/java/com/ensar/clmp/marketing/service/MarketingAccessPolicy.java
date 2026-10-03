package com.ensar.clmp.marketing.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.marketing.domain.MarketingAssignment;

/**
 * Ownership for marketing (authorization matrix "own marketing"): the assignment's consultant is
 * currently assigned to the caller's recruiter profile, so ownership follows reassignment.
 */
@Component
public class MarketingAccessPolicy {

    public boolean isOwningRecruiter(Consultant consultant, CurrentUser user) {
        return user.isRecruiter(consultant.getCurrentRecruiterId());
    }

    public boolean canView(MarketingAssignment assignment, CurrentUser user) {
        return switch (user.role()) {
            case ADMIN, MANAGER -> true;
            case RECRUITER -> isOwningRecruiter(assignment.getConsultant(), user);
            case HR_OPERATIONS -> false;
        };
    }

    public void assertCanView(MarketingAssignment assignment, CurrentUser user) {
        if (!canView(assignment, user)) {
            throw new AccessDeniedException("Marketing assignment is outside the caller's scope");
        }
    }

    /** Create, edit dates, and add notes: ADMIN or the owning RECRUITER (FR-040, FR-017). */
    public void assertCanEdit(Consultant consultant, CurrentUser user) {
        boolean allowed = switch (user.role()) {
            case ADMIN -> true;
            case RECRUITER -> isOwningRecruiter(consultant, user);
            case MANAGER, HR_OPERATIONS -> false;
        };
        if (!allowed) {
            throw new AccessDeniedException("Only ADMIN or the owning recruiter may change this assignment");
        }
    }
}
