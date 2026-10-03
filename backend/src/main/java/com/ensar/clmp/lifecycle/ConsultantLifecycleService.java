package com.ensar.clmp.lifecycle;

import static com.ensar.clmp.consultant.domain.ConsultantStatus.ACTIVE_PROJECT;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.BENCH;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.HOLD;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.INACTIVE;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.INTERVIEWING;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.MARKETING;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.PLACED;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.READY;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.consultant.service.ReadinessChecker;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.HistoryService;

/**
 * The single owner of consultant status rules (research R9): the manual transition table
 * (FR-031) and, from US4 on, the automatic transitions driven by marketing, submissions, and
 * placements (FR-032). Every change writes a history row in the caller's transaction.
 */
@Service
public class ConsultantLifecycleService {

    /** FR-031 manual transitions, in the order actions are offered. */
    private static final Map<ConsultantStatus, List<ConsultantStatus>> MANUAL = new EnumMap<>(ConsultantStatus.class);

    static {
        MANUAL.put(BENCH, List.of(READY, HOLD, INACTIVE));
        MANUAL.put(READY, List.of(BENCH, HOLD, INACTIVE));
        MANUAL.put(MARKETING, List.of(HOLD, INACTIVE));
        MANUAL.put(INTERVIEWING, List.of(HOLD, INACTIVE));
        MANUAL.put(PLACED, List.of(ACTIVE_PROJECT, INACTIVE));
        MANUAL.put(ACTIVE_PROJECT, List.of(BENCH, INACTIVE));
        MANUAL.put(HOLD, List.of(BENCH, READY, INACTIVE));
        MANUAL.put(INACTIVE, List.of(BENCH));
    }

    private static final Set<ConsultantStatus> REASON_REQUIRED = Set.of(HOLD, INACTIVE);

    private final ConsultantRepository consultants;
    private final HistoryService history;
    private final ReadinessChecker readiness;
    private final VersionGuard versionGuard;
    private final Clock clock;

    public ConsultantLifecycleService(ConsultantRepository consultants, HistoryService history,
            ReadinessChecker readiness, VersionGuard versionGuard, Clock clock) {
        this.consultants = consultants;
        this.history = history;
        this.readiness = readiness;
        this.versionGuard = versionGuard;
        this.clock = clock;
    }

    /** FR-031: ADMIN or HR_OPERATIONS changes a consultant's status by hand. */
    @Transactional
    public Consultant changeStatusManually(Long consultantId, ConsultantStatus target, String reason, Long version,
            CurrentUser actor) {
        if (!actor.isAny(Role.ADMIN, Role.HR_OPERATIONS)) {
            throw new AccessDeniedException("Only ADMIN and HR_OPERATIONS change consultant status manually");
        }
        Consultant consultant = consultants.findByIdForUpdate(consultantId).orElseThrow();
        versionGuard.check(consultant, version);

        ConsultantStatus from = consultant.getStatus();
        List<ConsultantStatus> allowed = MANUAL.getOrDefault(from, List.of());
        if (!allowed.contains(target)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSITION,
                    "Cannot change status from " + from + " to " + target + ".",
                    Map.of("currentStatus", from, "allowedTransitions", allowed));
        }
        if (REASON_REQUIRED.contains(target) && (reason == null || reason.isBlank())) {
            throw BusinessException.fieldError("reason", "A reason is required.");
        }
        if (target == READY) {
            List<String> missing = readiness.missingItems(consultant);
            if (!missing.isEmpty()) {
                throw new BusinessException(ErrorCode.READINESS_INCOMPLETE,
                        "The profile is not complete enough to mark Ready.", Map.of("missingItems", missing));
            }
        }
        if (target == ACTIVE_PROJECT) {
            guardActiveProject(consultant);
        }
        if (target == INACTIVE) {
            guardInactive(consultant);
        }

        apply(consultant, target, reason, actor);

        if (target == HOLD) {
            cascadeOnHold(consultant, actor);
        }
        if (target == INACTIVE) {
            cascadeOnInactive(consultant, actor);
        }
        return consultant;
    }

    /** Manual targets this caller may choose from the current status (empty for MANAGER and RECRUITER). */
    public List<ConsultantStatus> allowedManualTransitions(Consultant consultant, CurrentUser actor) {
        if (!actor.isAny(Role.ADMIN, Role.HR_OPERATIONS)) {
            return List.of();
        }
        return MANUAL.getOrDefault(consultant.getStatus(), List.of());
    }

    private void apply(Consultant consultant, ConsultantStatus target, String reason, CurrentUser actor) {
        ConsultantStatus from = consultant.getStatus();
        consultant.changeStatus(target, clock.instant());
        history.recordStatusChange(HistoryEntityType.CONSULTANT, consultant.getId(), consultant.getId(), null,
                from.name(), target.name(), reason, null, actor);
    }

    /** Refuses Inactive while open submissions exist. Filled in by US5. */
    private void guardInactive(Consultant consultant) {
        // No submissions exist before US5.
    }

    /** PLACED -> ACTIVE_PROJECT only on or after the placement start date. Filled in by US6. */
    private void guardActiveProject(Consultant consultant) {
        // No placements exist before US6.
    }

    /** Consultant on Hold puts an Active marketing assignment on Hold. Filled in by US4. */
    private void cascadeOnHold(Consultant consultant, CurrentUser actor) {
        // No marketing assignments exist before US4.
    }

    /** Consultant Inactive closes the open marketing assignment. Filled in by US4. */
    private void cascadeOnInactive(Consultant consultant, CurrentUser actor) {
        // No marketing assignments exist before US4.
    }
}
