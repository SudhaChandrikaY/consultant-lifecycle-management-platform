package com.ensar.clmp.consultant.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.common.error.FieldErrorItem;
import com.ensar.clmp.common.service.VersionGuard;
import com.ensar.clmp.common.web.PageResponse;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.consultant.web.ConsultantDetail;
import com.ensar.clmp.consultant.web.ConsultantListItem;
import com.ensar.clmp.consultant.web.ConsultantRequest;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.lifecycle.ConsultantLifecycleService;

/** Consultant profile workflows (FR-020–FR-028). Status rules live in ConsultantLifecycleService. */
@Service
public class ConsultantService {

    private final ConsultantRepository consultants;
    private final ConsultantAccessPolicy access;
    private final ConsultantLifecycleService lifecycle;
    private final ReadinessChecker readiness;
    private final HistoryService history;
    private final VersionGuard versionGuard;
    private final Clock clock;

    public ConsultantService(ConsultantRepository consultants, ConsultantAccessPolicy access,
            ConsultantLifecycleService lifecycle, ReadinessChecker readiness, HistoryService history,
            VersionGuard versionGuard, Clock clock) {
        this.consultants = consultants;
        this.access = access;
        this.lifecycle = lifecycle;
        this.readiness = readiness;
        this.history = history;
        this.versionGuard = versionGuard;
        this.clock = clock;
    }

    @Transactional
    public ConsultantDetail create(ConsultantRequest request, CurrentUser actor) {
        access.assertCanEditProfile(actor);
        if (consultants.existsByEmailIgnoreCase(request.email().trim())) {
            throw duplicateEmail();
        }
        Consultant consultant = consultants.saveAndFlush(new Consultant(request.toProfile(), actor.userId(),
                clock.instant()));
        history.recordCreated(HistoryEntityType.CONSULTANT, consultant.getId(), consultant.getId(), null,
                ConsultantStatus.BENCH.name(), actor);
        return toDetail(consultant, actor);
    }

    @Transactional
    public ConsultantDetail update(Long id, ConsultantRequest request, CurrentUser actor) {
        access.assertCanEditProfile(actor);
        Consultant consultant = consultants.findById(id).orElseThrow();
        versionGuard.check(consultant, request.version());
        if (consultants.existsByEmailIgnoreCaseAndIdNot(request.email().trim(), id)) {
            throw duplicateEmail();
        }
        List<String> changed = consultant.updateProfile(request.toProfile(), clock.instant());
        if (!changed.isEmpty()) {
            history.recordProfileUpdated(consultant.getId(), changed, actor);
        }
        consultants.flush();
        return toDetail(consultant, actor);
    }

    @Transactional
    public ConsultantDetail changeStatus(Long id, ConsultantStatus target, String reason, Long version,
            CurrentUser actor) {
        Consultant consultant = lifecycle.changeStatusManually(id, target, reason, version, actor);
        consultants.flush();
        return toDetail(consultant, actor);
    }

    @Transactional(readOnly = true)
    public ConsultantDetail getDetail(Long id, CurrentUser actor) {
        Consultant consultant = consultants.findById(id).orElseThrow();
        access.assertCanView(consultant, actor);
        return toDetail(consultant, actor);
    }

    /** Loads a consultant the caller may view; used by the history endpoint. */
    @Transactional(readOnly = true)
    public void assertCanView(Long id, CurrentUser actor) {
        access.assertCanView(consultants.findById(id).orElseThrow(), actor);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConsultantListItem> list(ConsultantFilter filter, Pageable pageable, CurrentUser actor) {
        return PageResponse.from(consultants.findAll(
                ConsultantSpecifications.matching(filter).and(ConsultantSpecifications.scopeFor(actor)), pageable),
                ConsultantListItem::from);
    }

    /** Counts through the same filter and scope as {@link #list} (research R13). */
    @Transactional(readOnly = true)
    public long count(ConsultantFilter filter, CurrentUser actor) {
        return consultants.count(
                ConsultantSpecifications.matching(filter).and(ConsultantSpecifications.scopeFor(actor)));
    }

    @Transactional(readOnly = true)
    public List<String> distinctSkills() {
        return consultants.findDistinctPrimarySkills();
    }

    ConsultantDetail toDetail(Consultant c, CurrentUser viewer) {
        ConsultantDetail.AssignedRecruiter recruiter = c.getCurrentRecruiter() == null ? null
                : new ConsultantDetail.AssignedRecruiter(c.getCurrentRecruiter().getId(),
                        c.getCurrentRecruiter().getFullName(), c.getCurrentRecruiter().getStatus());
        ConsultantDetail.Contact contact = access.canSeeContact(c, viewer)
                ? new ConsultantDetail.Contact(c.getEmail(), c.getPhone(), c.getVisaExpirationDate(), c.getNotes())
                : null;
        return new ConsultantDetail(c.getId(), c.getFirstName(), c.getLastName(), c.getCity(), c.getState(),
                c.getPrimarySkill(), c.getAdditionalSkills(), c.getYearsExperience(), c.getVisaType(), c.getStatus(),
                c.needsReassignment(), recruiter, contact, lifecycle.allowedManualTransitions(c, viewer),
                readiness.missingItems(c), c.getVersion());
    }

    private static BusinessException duplicateEmail() {
        return new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email is already in use.", Map.of(),
                List.of(new FieldErrorItem("email", "Email is already in use by another consultant.")));
    }
}
