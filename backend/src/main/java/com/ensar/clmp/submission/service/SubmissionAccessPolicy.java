package com.ensar.clmp.submission.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.submission.domain.Submission;

/**
 * FR-063/FR-064/FR-070. A RECRUITER owns a submission when they are its recruiter or the
 * consultant is now assigned to them; only the submission's own recruiter (or ADMIN) may turn it
 * into a placement. MANAGER is read-only; HR_OPERATIONS sees nothing.
 */
@Component
public class SubmissionAccessPolicy {

    public boolean isOwn(Submission s, CurrentUser user) {
        return user.isRecruiter(s.getRecruiter().getId()) || user.isRecruiter(s.getConsultant().getCurrentRecruiterId());
    }

    public boolean canView(Submission s, CurrentUser user) {
        return switch (user.role()) {
            case ADMIN, MANAGER -> true;
            case RECRUITER -> isOwn(s, user);
            case HR_OPERATIONS -> false;
        };
    }

    public void assertCanView(Submission s, CurrentUser user) {
        if (!canView(s, user)) {
            throw new AccessDeniedException("Submission is outside the caller's scope");
        }
    }

    public boolean canUpdate(Submission s, CurrentUser user) {
        return switch (user.role()) {
            case ADMIN -> true;
            case RECRUITER -> isOwn(s, user);
            case MANAGER, HR_OPERATIONS -> false;
        };
    }

    public void assertCanUpdate(Submission s, CurrentUser user) {
        if (!canUpdate(s, user)) {
            throw new AccessDeniedException("Only ADMIN or an owning recruiter may update this submission");
        }
    }

    /** FR-070: ADMIN, or the RECRUITER who is the submission's recruiter (not merely the consultant's). */
    public boolean canCreatePlacementFrom(Submission s, CurrentUser user) {
        return switch (user.role()) {
            case ADMIN -> true;
            case RECRUITER -> user.isRecruiter(s.getRecruiter().getId());
            case MANAGER, HR_OPERATIONS -> false;
        };
    }
}
