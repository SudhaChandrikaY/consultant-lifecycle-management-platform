package com.ensar.clmp.submission.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
import com.ensar.clmp.history.service.HistoryQueryService;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.lifecycle.ConsultantLifecycleService;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;
import com.ensar.clmp.reference.domain.Client;
import com.ensar.clmp.reference.domain.Vendor;
import com.ensar.clmp.reference.service.CounterpartyService;
import com.ensar.clmp.reference.service.NameNormalizer;
import com.ensar.clmp.submission.domain.Submission;
import com.ensar.clmp.submission.domain.SubmissionDuplicateRef;
import com.ensar.clmp.submission.domain.SubmissionDuplicateRefRepository;
import com.ensar.clmp.submission.domain.SubmissionNote;
import com.ensar.clmp.submission.domain.SubmissionNoteRepository;
import com.ensar.clmp.submission.domain.SubmissionRepository;
import com.ensar.clmp.submission.domain.SubmissionStatus;
import com.ensar.clmp.submission.web.DuplicateSummary;
import com.ensar.clmp.submission.web.SubmissionCreateRequest;
import com.ensar.clmp.submission.web.SubmissionDetail;
import com.ensar.clmp.submission.web.SubmissionListItem;
import com.ensar.clmp.submission.web.SubmissionSummary;

/**
 * Submission workflow (FR-050–FR-064): create, status progression, and notes only. There is no
 * general edit by design (research R19-7).
 */
@Service
public class SubmissionService {

    private static final Set<ConsultantStatus> ELIGIBLE = Set.of(ConsultantStatus.READY, ConsultantStatus.MARKETING,
            ConsultantStatus.INTERVIEWING);

    private final SubmissionRepository submissions;
    private final SubmissionNoteRepository notes;
    private final SubmissionDuplicateRefRepository duplicateRefs;
    private final ConsultantRepository consultants;
    private final RecruiterRepository recruiters;
    private final CounterpartyService counterparties;
    private final SubmissionAccessPolicy access;
    private final SubmissionTransitions transitions;
    private final ConsultantLifecycleService lifecycle;
    private final HistoryService history;
    private final HistoryQueryService historyQuery;
    private final VersionGuard versionGuard;
    private final OrgTime orgTime;

    public SubmissionService(SubmissionRepository submissions, SubmissionNoteRepository notes,
            SubmissionDuplicateRefRepository duplicateRefs, ConsultantRepository consultants,
            RecruiterRepository recruiters, CounterpartyService counterparties, SubmissionAccessPolicy access,
            SubmissionTransitions transitions, ConsultantLifecycleService lifecycle, HistoryService history,
            HistoryQueryService historyQuery, VersionGuard versionGuard, OrgTime orgTime) {
        this.submissions = submissions;
        this.notes = notes;
        this.duplicateRefs = duplicateRefs;
        this.consultants = consultants;
        this.recruiters = recruiters;
        this.counterparties = counterparties;
        this.access = access;
        this.transitions = transitions;
        this.lifecycle = lifecycle;
        this.history = history;
        this.historyQuery = historyQuery;
        this.versionGuard = versionGuard;
        this.orgTime = orgTime;
    }

    @Transactional
    public SubmissionDetail create(SubmissionCreateRequest request, CurrentUser actor) {
        Consultant consultant = consultants.findByIdForUpdate(request.consultantId())
                .orElseThrow(() -> BusinessException.fieldError("consultantId", "The consultant does not exist."));
        if (!(actor.is(Role.ADMIN) || actor.isRecruiter(consultant.getCurrentRecruiterId()))) {
            throw new AccessDeniedException("Only ADMIN or the consultant's recruiter may submit this consultant");
        }
        if (!ELIGIBLE.contains(consultant.getStatus())) {
            throw new BusinessException(ErrorCode.CONSULTANT_NOT_ELIGIBLE,
                    "Only Ready, Marketing, or Interviewing consultants can be submitted.",
                    Map.of("consultantStatus", consultant.getStatus()));
        }
        Recruiter recruiter = resolveRecruiter(request.recruiterId(), consultant, actor);
        boolean submitNow = Boolean.TRUE.equals(request.submitNow());
        LocalDate submittedDate = submitNow ? checkSubmittedDate(request.submittedDate()) : null;
        Vendor vendor = counterparties.resolveVendor(request.vendorId(), request.vendorName(), actor);
        Client client = counterparties.resolveClient(request.clientId(), request.clientName(), actor);

        List<Submission> duplicates = submissions.findDuplicates(consultant.getId(), vendor.getId(), client.getId(),
                NameNormalizer.normalize(request.jobTitle()));
        boolean acknowledged = Boolean.TRUE.equals(request.acknowledgeDuplicate());
        if (!duplicates.isEmpty() && !acknowledged) {
            throw new BusinessException(ErrorCode.DUPLICATE_SUBMISSION,
                    "This consultant was already submitted to the same vendor, client, and job title.",
                    Map.of("duplicates", duplicates.stream().map(SubmissionService::toDuplicate).toList()));
        }

        Submission submission = new Submission(consultant, recruiter, vendor, client, request.jobTitle(),
                request.billRate(), submitNow ? SubmissionStatus.SUBMITTED : SubmissionStatus.DRAFT, submittedDate,
                actor.userId(), orgTime.now());
        if (!duplicates.isEmpty()) {
            submission.acknowledgeDuplicate(actor.userId(), actor.displayName(), orgTime.now());
        }
        submission = submissions.saveAndFlush(submission);
        for (Submission earlier : duplicates) {
            duplicateRefs.save(new SubmissionDuplicateRef(submission.getId(), earlier.getId()));
        }

        history.recordCreated(HistoryEntityType.SUBMISSION, submission.getId(), consultant.getId(), recruiter.getId(),
                SubmissionStatus.DRAFT.name(), actor);
        if (submitNow) {
            history.recordStatusChange(HistoryEntityType.SUBMISSION, submission.getId(), consultant.getId(),
                    recruiter.getId(), SubmissionStatus.DRAFT.name(), SubmissionStatus.SUBMITTED.name(), null, null,
                    actor);
        }
        if (request.note() != null && !request.note().isBlank()) {
            notes.save(new SubmissionNote(submission.getId(), request.note().trim(), actor.userId(),
                    actor.displayName(), orgTime.now()));
        }
        return toDetail(submission, actor);
    }

    @Transactional
    public SubmissionDetail changeStatus(Long id, SubmissionStatus target, String note, LocalDate submittedDate,
            Long version, CurrentUser actor) {
        Submission submission = submissions.findDetailedById(id).orElseThrow();
        consultants.findByIdForUpdate(submission.getConsultant().getId()).orElseThrow();
        access.assertCanUpdate(submission, actor);
        versionGuard.check(submission, version);
        SubmissionStatus from = submission.getStatus();
        transitions.validate(from, target);
        if (target == SubmissionStatus.SUBMITTED) {
            submission.setSubmittedDate(checkSubmittedDate(submittedDate));
        }
        submission.changeStatus(target);
        history.recordStatusChange(HistoryEntityType.SUBMISSION, id, submission.getConsultant().getId(),
                submission.getRecruiter().getId(), from.name(), target.name(), null, note, actor);
        lifecycle.onSubmissionStatusChanged(submission, from, target, actor);
        submissions.flush();
        return toDetail(submission, actor);
    }

    @Transactional
    public NoteResponse addNote(Long id, String body, CurrentUser actor) {
        Submission submission = submissions.findDetailedById(id).orElseThrow();
        access.assertCanUpdate(submission, actor);
        SubmissionNote note = notes.save(new SubmissionNote(id, body.trim(), actor.userId(), actor.displayName(),
                orgTime.now()));
        history.recordNoteAdded(HistoryEntityType.SUBMISSION, id, submission.getConsultant().getId(),
                submission.getRecruiter().getId(), actor);
        return new NoteResponse(note.getId(), note.getBody(), note.getAuthorDisplayName(), note.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public SubmissionDetail getDetail(Long id, CurrentUser actor) {
        Submission submission = submissions.findDetailedById(id).orElseThrow();
        access.assertCanView(submission, actor);
        return toDetail(submission, actor);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionListItem> list(SubmissionFilter filter, Pageable pageable, CurrentUser actor) {
        return PageResponse.from(submissions.findAll(
                SubmissionSpecifications.matching(filter).and(SubmissionSpecifications.scopeFor(actor)), pageable),
                SubmissionListItem::from);
    }

    /** Counts through the same filter and scope as {@link #list} (research R13). */
    @Transactional(readOnly = true)
    public long count(SubmissionFilter filter, CurrentUser actor) {
        return submissions.count(SubmissionSpecifications.matching(filter).and(SubmissionSpecifications.scopeFor(actor)));
    }

    /** Submissions panel rows for a consultant the caller may already view. */
    @Transactional(readOnly = true)
    public List<SubmissionSummary> summariesFor(Long consultantId) {
        return submissions.findByConsultant_IdOrderByCreatedAtDescIdDesc(consultantId).stream()
                .map(SubmissionSummary::from).toList();
    }

    private Recruiter resolveRecruiter(Long requestedId, Consultant consultant, CurrentUser actor) {
        Recruiter current = consultant.getCurrentRecruiter();
        if (requestedId == null || (current != null && requestedId.equals(current.getId()))) {
            if (current == null) {
                throw BusinessException.fieldError("recruiterId", "The consultant has no assigned recruiter.");
            }
            return current;
        }
        if (!actor.is(Role.ADMIN)) {
            throw BusinessException.fieldError("recruiterId",
                    "Recruiters submit consultants under their own name.");
        }
        Recruiter recruiter = recruiters.findById(requestedId)
                .orElseThrow(() -> BusinessException.fieldError("recruiterId", "The recruiter does not exist."));
        if (!recruiter.isActive()) {
            throw BusinessException.fieldError("recruiterId", "The recruiter must be active.");
        }
        return recruiter;
    }

    /** FR-054: defaults to today and is never in the future. */
    private LocalDate checkSubmittedDate(LocalDate requested) {
        LocalDate today = orgTime.today();
        LocalDate date = requested == null ? today : requested;
        if (date.isAfter(today)) {
            throw BusinessException.fieldError("submittedDate", "Submitted date cannot be in the future.");
        }
        return date;
    }

    private SubmissionDetail toDetail(Submission s, CurrentUser viewer) {
        SubmissionDetail.DuplicateAcknowledgement ack = s.isDuplicateAcknowledged()
                ? new SubmissionDetail.DuplicateAcknowledgement(s.getDuplicateAcknowledgedByName(),
                        s.getDuplicateAcknowledgedAt(), duplicateRefs.findBySubmissionId(s.getId()).stream()
                                .map(SubmissionDuplicateRef::getEarlierSubmissionId).toList())
                : null;
        List<NoteResponse> noteList = notes.findBySubmissionIdOrderByCreatedAtAscIdAsc(s.getId()).stream()
                .map(n -> new NoteResponse(n.getId(), n.getBody(), n.getAuthorDisplayName(), n.getCreatedAt()))
                .toList();
        var timeline = historyQuery.forEntity(HistoryEntityType.SUBMISSION, s.getId(), viewer, PageRequest.of(0, 200))
                .getContent();
        List<SubmissionStatus> allowed = access.canUpdate(s, viewer) ? transitions.allowedFrom(s.getStatus()) : List.of();
        boolean canPlace = s.getStatus() == SubmissionStatus.OFFER && access.canCreatePlacementFrom(s, viewer);
        return new SubmissionDetail(s.getId(), new PersonRef(s.getConsultant().getId(), s.getConsultant().getFullName()),
                new PersonRef(s.getRecruiter().getId(), s.getRecruiter().getFullName()),
                new NamedRef(s.getVendor().getId(), s.getVendor().getName()),
                new NamedRef(s.getClient().getId(), s.getClient().getName()), s.getJobTitle(), s.getSubmittedDate(),
                s.getBillRate(), s.getStatus(), ack, noteList, timeline, allowed, canPlace, s.getVersion());
    }

    private static DuplicateSummary toDuplicate(Submission s) {
        return new DuplicateSummary(s.getId(), s.getStatus(), s.getSubmittedDate(), s.getCreatedAt(),
                s.getRecruiter().getFullName());
    }
}
