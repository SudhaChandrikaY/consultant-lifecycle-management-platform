package com.ensar.clmp.consultant.service;

import java.time.Clock;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.common.service.VersionGuard;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;

/** Assigns or reassigns a consultant's single primary recruiter (FR-033, FR-034). */
@Service
public class ConsultantAssignmentService {

    private final ConsultantRepository consultants;
    private final RecruiterRepository recruiters;
    private final HistoryService history;
    private final VersionGuard versionGuard;
    private final Clock clock;

    public ConsultantAssignmentService(ConsultantRepository consultants, RecruiterRepository recruiters,
            HistoryService history, VersionGuard versionGuard, Clock clock) {
        this.consultants = consultants;
        this.recruiters = recruiters;
        this.history = history;
        this.versionGuard = versionGuard;
        this.clock = clock;
    }

    @Transactional
    public Consultant assign(Long consultantId, Long recruiterId, Long version, CurrentUser actor) {
        if (!actor.isAny(Role.ADMIN, Role.MANAGER)) {
            throw new AccessDeniedException("Only ADMIN and MANAGER assign recruiters");
        }
        Consultant consultant = consultants.findByIdForUpdate(consultantId).orElseThrow();
        versionGuard.check(consultant, version);
        Recruiter recruiter = recruiters.findById(recruiterId)
                .orElseThrow(() -> BusinessException.fieldError("recruiterId", "The selected recruiter does not exist."));
        if (!recruiter.isActive()) {
            throw new BusinessException(ErrorCode.RECRUITER_INACTIVE,
                    "The recruiter is inactive and cannot receive assignments.");
        }
        Recruiter previous = consultant.getCurrentRecruiter();
        if (previous != null && previous.getId().equals(recruiterId)) {
            return consultant;
        }
        consultant.assignRecruiter(recruiter, clock.instant());
        history.recordRecruiterAssignment(consultant.getId(), previous == null ? null : previous.getFullName(),
                recruiter.getFullName(), actor);
        transferOpenMarketingOwnership(consultant, recruiter, actor);
        return consultant;
    }

    /** The open marketing assignment follows the consultant to the new recruiter. Filled in by US4. */
    private void transferOpenMarketingOwnership(Consultant consultant, Recruiter newRecruiter, CurrentUser actor) {
        // No marketing assignments exist before US4.
    }
}
