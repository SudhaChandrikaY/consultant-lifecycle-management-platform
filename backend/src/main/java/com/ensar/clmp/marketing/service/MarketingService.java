package com.ensar.clmp.marketing.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.common.service.VersionGuard;
import com.ensar.clmp.common.time.OrgTime;
import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.NoteResponse;
import com.ensar.clmp.common.web.PageResponse;
import com.ensar.clmp.common.web.PersonRef;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.domain.TriggerEvent;
import com.ensar.clmp.history.service.HistoryQueryService;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.history.web.HistoryEntry;
import com.ensar.clmp.lifecycle.ConsultantLifecycleService;
import com.ensar.clmp.marketing.domain.MarketingAssignment;
import com.ensar.clmp.marketing.domain.MarketingAssignmentRepository;
import com.ensar.clmp.marketing.domain.MarketingNote;
import com.ensar.clmp.marketing.domain.MarketingNoteRepository;
import com.ensar.clmp.marketing.domain.MarketingStatus;
import com.ensar.clmp.marketing.web.MarketingCreateRequest;
import com.ensar.clmp.marketing.web.MarketingDetail;
import com.ensar.clmp.marketing.web.MarketingListItem;
import com.ensar.clmp.marketing.web.MarketingSummary;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;

/**
 * Marketing workflow (FR-040–FR-045). Per-consultant invariants are checked under a consultant
 * row lock (research R10); consultant status effects go through ConsultantLifecycleService.
 */
@Service
public class MarketingService {

    private static final java.util.Set<ConsultantStatus> REOPENABLE_FROM_HOLD = java.util.Set.of(
            ConsultantStatus.READY, ConsultantStatus.MARKETING, ConsultantStatus.INTERVIEWING);

    private final MarketingAssignmentRepository assignments;
    private final MarketingNoteRepository notes;
    private final ConsultantRepository consultants;
    private final RecruiterRepository recruiters;
    private final MarketingAccessPolicy access;
    private final MarketingTransitions transitions;
    private final ConsultantLifecycleService lifecycle;
    private final HistoryService history;
    private final HistoryQueryService historyQuery;
    private final VersionGuard versionGuard;
    private final OrgTime orgTime;

    public MarketingService(MarketingAssignmentRepository assignments, MarketingNoteRepository notes,
            ConsultantRepository consultants, RecruiterRepository recruiters, MarketingAccessPolicy access,
            MarketingTransitions transitions, ConsultantLifecycleService lifecycle, HistoryService history,
            HistoryQueryService historyQuery, VersionGuard versionGuard, OrgTime orgTime) {
        this.assignments = assignments;
        this.notes = notes;
        this.consultants = consultants;
        this.recruiters = recruiters;
        this.access = access;
        this.transitions = transitions;
        this.lifecycle = lifecycle;
        this.history = history;
        this.historyQuery = historyQuery;
        this.versionGuard = versionGuard;
        this.orgTime = orgTime;
    }

    @Transactional
    public MarketingDetail create(MarketingCreateRequest request, CurrentUser actor) {
        Consultant consultant = consultants.findByIdForUpdate(request.consultantId())
                .orElseThrow(() -> BusinessException.fieldError("consultantId", "The consultant does not exist."));
        access.assertCanEdit(consultant, actor);
        assignments.findOpenFor(consultant.getId()).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.OPEN_ASSIGNMENT_EXISTS,
                    "This consultant already has an open marketing assignment.",
                    Map.of("existingRecordId", existing.getId()));
        });
        if (consultant.getStatus() != ConsultantStatus.READY) {
            throw notEligible(consultant, "The consultant must be Ready to start marketing.");
        }
        validateDates(request.startDate(), request.targetDate());
        Recruiter owner = resolveOwner(request.ownerRecruiterId(), consultant, actor);

        MarketingAssignment assignment = assignments.saveAndFlush(new MarketingAssignment(consultant, owner,
                request.startDate(), request.targetDate(), actor.userId(), orgTime.now()));
        history.recordCreated(HistoryEntityType.MARKETING_ASSIGNMENT, assignment.getId(), consultant.getId(),
                owner.getId(), MarketingStatus.DRAFT.name(), actor);
        return toDetail(assignment, actor);
    }

    @Transactional
    public MarketingDetail updateDates(Long id, LocalDate startDate, LocalDate targetDate, Long version,
            CurrentUser actor) {
        MarketingAssignment assignment = assignments.findById(id).orElseThrow();
        access.assertCanEdit(assignment.getConsultant(), actor);
        versionGuard.check(assignment, version);
        if (assignment.getStatus() == MarketingStatus.CLOSED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, "A closed assignment's dates cannot be changed.");
        }
        validateDates(startDate, targetDate);
        Long consultantId = assignment.getConsultant().getId();
        Long ownerId = assignment.getOwnerRecruiter().getId();
        if (!Objects.equals(assignment.getStartDate(), startDate)) {
            history.recordFieldEdit(HistoryEntityType.MARKETING_ASSIGNMENT, id, consultantId, ownerId, "startDate",
                    assignment.getStartDate().toString(), startDate.toString(), actor);
        }
        if (!Objects.equals(assignment.getTargetDate(), targetDate)) {
            history.recordFieldEdit(HistoryEntityType.MARKETING_ASSIGNMENT, id, consultantId, ownerId, "targetDate",
                    assignment.getTargetDate().toString(), targetDate.toString(), actor);
        }
        assignment.changeDates(startDate, targetDate);
        assignments.flush();
        return toDetail(assignment, actor);
    }

    @Transactional
    public MarketingDetail transition(Long id, MarketingStatus target, String reason, Long version,
            CurrentUser actor) {
        MarketingAssignment assignment = assignments.findById(id).orElseThrow();
        Consultant consultant = consultants.findByIdForUpdate(assignment.getConsultant().getId()).orElseThrow();
        versionGuard.check(assignment, version);
        MarketingStatus from = assignment.getStatus();
        transitions.validate(from, target, assignment.getCloseReason(), reason, actor.role(),
                access.isOwningRecruiter(consultant, actor));

        if (target == MarketingStatus.ACTIVE) {
            switch (from) {
                case DRAFT -> requireConsultant(consultant, ConsultantStatus.READY,
                        "The consultant must be Ready to activate marketing.");
                case HOLD -> {
                    if (!REOPENABLE_FROM_HOLD.contains(consultant.getStatus())) {
                        throw notEligible(consultant,
                                "The consultant must be Ready, Marketing, or Interviewing to reopen marketing.");
                    }
                }
                case CLOSED -> {
                    requireConsultant(consultant, ConsultantStatus.READY,
                            "The consultant must be Ready to reopen marketing.");
                    assignments.findOpenFor(consultant.getId()).ifPresent(other -> {
                        throw new BusinessException(ErrorCode.OPEN_ASSIGNMENT_EXISTS,
                                "This consultant already has an open marketing assignment.",
                                Map.of("existingRecordId", other.getId()));
                    });
                }
                default -> {
                }
            }
        }

        String storedReason = reason == null || reason.isBlank() ? null : reason.trim();
        assignment.changeStatus(target, storedReason, orgTime.now());
        history.recordStatusChange(HistoryEntityType.MARKETING_ASSIGNMENT, id, consultant.getId(),
                assignment.getOwnerRecruiter().getId(), from.name(), target.name(), storedReason, null, actor);

        if (target == MarketingStatus.ACTIVE) {
            lifecycle.onMarketingActivatedOrReopened(assignment,
                    from == MarketingStatus.DRAFT ? TriggerEvent.MARKETING_ACTIVATED : TriggerEvent.MARKETING_REOPENED,
                    actor);
        } else if (target == MarketingStatus.CLOSED) {
            lifecycle.onMarketingClosed(assignment, actor);
        }
        assignments.flush();
        return toDetail(assignment, actor);
    }

    @Transactional
    public NoteResponse addNote(Long id, String body, CurrentUser actor) {
        MarketingAssignment assignment = assignments.findById(id).orElseThrow();
        access.assertCanEdit(assignment.getConsultant(), actor);
        MarketingNote note = notes.save(new MarketingNote(id, body.trim(), actor.userId(), actor.displayName(),
                orgTime.now()));
        history.recordNoteAdded(HistoryEntityType.MARKETING_ASSIGNMENT, id, assignment.getConsultant().getId(),
                assignment.getOwnerRecruiter().getId(), actor);
        return toNote(note);
    }

    @Transactional(readOnly = true)
    public MarketingDetail getDetail(Long id, CurrentUser actor) {
        MarketingAssignment assignment = assignments.findById(id).orElseThrow();
        access.assertCanView(assignment, actor);
        return toDetail(assignment, actor);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketingListItem> list(MarketingFilter filter, Pageable pageable, CurrentUser actor) {
        LocalDate today = orgTime.today();
        Page<MarketingAssignment> page = assignments.findAll(MarketingSpecifications.matching(filter, today)
                .and(MarketingSpecifications.scopeFor(actor)), pageable);
        return PageResponse.from(page, a -> MarketingListItem.from(a, today));
    }

    @Transactional(readOnly = true)
    public Page<HistoryEntry> history(Long id, CurrentUser actor, Pageable pageable) {
        access.assertCanView(assignments.findById(id).orElseThrow(), actor);
        return historyQuery.forEntity(HistoryEntityType.MARKETING_ASSIGNMENT, id, actor, pageable);
    }

    /** The consultant's open assignment for the consultant detail page, or null. */
    @Transactional(readOnly = true)
    public MarketingSummary currentSummaryFor(Long consultantId) {
        LocalDate today = orgTime.today();
        return assignments.findOpenFor(consultantId)
                .map(a -> new MarketingSummary(a.getId(), a.getStatus(), a.getTargetDate(), a.isOverdue(today)))
                .orElse(null);
    }

    private Recruiter resolveOwner(Long requestedOwnerId, Consultant consultant, CurrentUser actor) {
        Recruiter current = consultant.getCurrentRecruiter();
        if (requestedOwnerId == null || (current != null && requestedOwnerId.equals(current.getId()))) {
            if (current == null || !current.isActive()) {
                throw BusinessException.fieldError("ownerRecruiterId",
                        "The consultant has no active assigned recruiter to own this assignment.");
            }
            return current;
        }
        if (!actor.is(Role.ADMIN)) {
            throw BusinessException.fieldError("ownerRecruiterId",
                    "Recruiters own the assignments they create for their consultants.");
        }
        Recruiter owner = recruiters.findById(requestedOwnerId)
                .orElseThrow(() -> BusinessException.fieldError("ownerRecruiterId", "The recruiter does not exist."));
        if (!owner.isActive()) {
            throw BusinessException.fieldError("ownerRecruiterId", "The owning recruiter must be active.");
        }
        return owner;
    }

    private static void validateDates(LocalDate start, LocalDate target) {
        if (target.isBefore(start)) {
            throw BusinessException.fieldError("targetDate", "Target date must be on or after start date.");
        }
    }

    private static void requireConsultant(Consultant consultant, ConsultantStatus required, String message) {
        if (consultant.getStatus() != required) {
            throw notEligible(consultant, message);
        }
    }

    private static BusinessException notEligible(Consultant consultant, String message) {
        return new BusinessException(ErrorCode.CONSULTANT_NOT_ELIGIBLE, message,
                Map.of("consultantStatus", consultant.getStatus()));
    }

    private MarketingDetail toDetail(MarketingAssignment a, CurrentUser viewer) {
        LocalDate today = orgTime.today();
        List<NoteResponse> noteList = notes.findByMarketingAssignmentIdOrderByCreatedAtAscIdAsc(a.getId()).stream()
                .map(MarketingService::toNote).toList();
        List<MarketingStatus> allowed = transitions.allowedFor(a.getStatus(), a.getCloseReason(), viewer.role(),
                access.isOwningRecruiter(a.getConsultant(), viewer));
        return new MarketingDetail(a.getId(), new PersonRef(a.getConsultant().getId(), a.getConsultant().getFullName()),
                new PersonRef(a.getOwnerRecruiter().getId(), a.getOwnerRecruiter().getFullName()),
                new NamedRef(a.getOwnerTeam().getId(), a.getOwnerTeam().getName()), a.getStartDate(),
                a.getTargetDate(), a.getStatus(), a.isOverdue(today), a.getHoldReason(), a.getCloseReason(), noteList,
                allowed, a.getVersion());
    }

    private static NoteResponse toNote(MarketingNote n) {
        return new NoteResponse(n.getId(), n.getBody(), n.getAuthorDisplayName(), n.getCreatedAt());
    }
}
