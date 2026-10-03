package com.ensar.clmp.placement.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.placement.domain.Placement;
import com.ensar.clmp.submission.domain.Submission;
import com.ensar.clmp.submission.service.SubmissionAccessPolicy;

/** FR-070, FR-073, FR-074, FR-077. HR_OPERATIONS is always refused. */
@Component
public class PlacementAccessPolicy {

    private final SubmissionAccessPolicy submissionAccess;

    public PlacementAccessPolicy(SubmissionAccessPolicy submissionAccess) {
        this.submissionAccess = submissionAccess;
    }

    public boolean canView(Placement p, CurrentUser user) {
        return switch (user.role()) {
            case ADMIN, MANAGER -> true;
            case RECRUITER -> user.isRecruiter(p.getRecruiter().getId());
            case HR_OPERATIONS -> false;
        };
    }

    public void assertCanView(Placement p, CurrentUser user) {
        if (!canView(p, user)) {
            throw new AccessDeniedException("Placement is outside the caller's scope");
        }
    }

    public void assertCanCreateFrom(Submission s, CurrentUser user) {
        if (!submissionAccess.canCreatePlacementFrom(s, user)) {
            throw new AccessDeniedException("Only ADMIN or the submission's recruiter may create this placement");
        }
    }

    public void assertCanEdit(CurrentUser user) {
        if (user.role() != com.ensar.clmp.auth.domain.Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN edits placements");
        }
    }
}
