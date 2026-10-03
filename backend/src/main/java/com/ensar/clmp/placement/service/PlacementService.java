package com.ensar.clmp.placement.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.AppUser;
import com.ensar.clmp.auth.domain.AppUserRepository;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.common.service.VersionGuard;
import com.ensar.clmp.common.time.OrgTime;
import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.PageResponse;
import com.ensar.clmp.common.web.PersonRef;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.HistoryQueryService;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.lifecycle.ConsultantLifecycleService;
import com.ensar.clmp.placement.domain.Placement;
import com.ensar.clmp.placement.domain.PlacementRepository;
import com.ensar.clmp.placement.web.PlacementCreateRequest;
import com.ensar.clmp.placement.web.PlacementCreated;
import com.ensar.clmp.placement.web.PlacementDetail;
import com.ensar.clmp.placement.web.PlacementDraft;
import com.ensar.clmp.placement.web.PlacementListItem;
import com.ensar.clmp.placement.web.PlacementPatchRequest;
import com.ensar.clmp.placement.web.PlacementSummary;
import com.ensar.clmp.submission.domain.Submission;
import com.ensar.clmp.submission.domain.SubmissionRepository;
import com.ensar.clmp.submission.domain.SubmissionStatus;

/** Offer → placement conversion (FR-070–FR-077), atomic under a consultant row lock (research R10). */
@Service
public class PlacementService {

    private final PlacementRepository placements;
    private final SubmissionRepository submissions;
    private final ConsultantRepository consultants;
    private final AppUserRepository users;
    private final PlacementAccessPolicy access;
    private final ConsultantLifecycleService lifecycle;
    private final HistoryService history;
    private final HistoryQueryService historyQuery;
    private final VersionGuard versionGuard;
    private final OrgTime orgTime;

    public PlacementService(PlacementRepository placements, SubmissionRepository submissions,
            ConsultantRepository consultants, AppUserRepository users, PlacementAccessPolicy access,
            ConsultantLifecycleService lifecycle, HistoryService history, HistoryQueryService historyQuery,
            VersionGuard versionGuard, OrgTime orgTime) {
        this.placements = placements;
        this.submissions = submissions;
        this.consultants = consultants;
        this.users = users;
        this.access = access;
        this.lifecycle = lifecycle;
        this.history = history;
        this.historyQuery = historyQuery;
        this.versionGuard = versionGuard;
        this.orgTime = orgTime;
    }

    @Transactional(readOnly = true)
    public PlacementDraft draft(Long submissionId, CurrentUser actor) {
        Submission s = submissions.findDetailedById(submissionId).orElseThrow();
        access.assertCanCreateFrom(s, actor);
        requireOffer(s);
        return new PlacementDraft(s.getId(), new PersonRef(s.getConsultant().getId(), s.getConsultant().getFullName()),
                new PersonRef(s.getRecruiter().getId(), s.getRecruiter().getFullName()),
                new NamedRef(s.getVendor().getId(), s.getVendor().getName()),
                new NamedRef(s.getClient().getId(), s.getClient().getName()), s.getJobTitle(), s.getBillRate(),
                s.getSubmittedDate());
    }

    /** One transaction: placement, submission → Placed, consultant → Placed, marketing closed (FR-072). */
    @Transactional
    public PlacementCreated create(PlacementCreateRequest request, CurrentUser actor) {
        Submission submission = submissions.findDetailedById(request.submissionId())
                .orElseThrow(() -> BusinessException.fieldError("submissionId", "The submission does not exist."));
        Consultant consultant = consultants.findByIdForUpdate(submission.getConsultant().getId()).orElseThrow();
        access.assertCanCreateFrom(submission, actor);
        requireOffer(submission);
        if (consultant.getStatus() == ConsultantStatus.PLACED || consultant.getStatus() == ConsultantStatus.ACTIVE_PROJECT) {
            throw new BusinessException(ErrorCode.ALREADY_PLACED, "This consultant is already placed.");
        }
        checkStartDate(request.startDate(), submission);

        Placement placement = placements.saveAndFlush(new Placement(submission, request.startDate(), request.billRate(),
                request.contractTermMonths(), actor.userId(), orgTime.now()));
        history.recordCreated(HistoryEntityType.PLACEMENT, placement.getId(), consultant.getId(),
                placement.getRecruiter().getId(), null, actor);
        lifecycle.onPlacementCreated(placement, submission, actor);
        placements.flush();

        List<PlacementCreated.OtherOpenSubmission> others = submissions
                .findByConsultant_IdAndStatusInOrderById(consultant.getId(), SubmissionStatus.OPEN).stream()
                .filter(s -> !s.getId().equals(submission.getId()))
                .map(s -> new PlacementCreated.OtherOpenSubmission(s.getId(), s.getVendor().getName(),
                        s.getClient().getName(), s.getJobTitle(), s.getStatus(), s.getVersion()))
                .toList();
        return new PlacementCreated(toDetail(placement, actor), others);
    }

    /** ADMIN only; each changed field writes a FIELD_EDIT history row (FR-074). */
    @Transactional
    public PlacementDetail edit(Long id, PlacementPatchRequest patch, CurrentUser actor) {
        access.assertCanEdit(actor);
        Placement p = placements.findDetailedById(id).orElseThrow();
        versionGuard.check(p, patch.version());
        Long consultantId = p.getConsultant().getId();
        Long recruiterId = p.getRecruiter().getId();
        if (patch.startDate() != null && !patch.startDate().equals(p.getStartDate())) {
            checkStartDate(patch.startDate(), p.getSubmission());
            history.recordFieldEdit(HistoryEntityType.PLACEMENT, id, consultantId, recruiterId, "startDate",
                    p.getStartDate().toString(), patch.startDate().toString(), actor);
            p.changeStartDate(patch.startDate());
        }
        if (patch.billRate() != null && patch.billRate().compareTo(p.getBillRate()) != 0) {
            history.recordFieldEdit(HistoryEntityType.PLACEMENT, id, consultantId, recruiterId, "billRate",
                    p.getBillRate().toPlainString(), patch.billRate().toPlainString(), actor);
            p.changeBillRate(patch.billRate());
        }
        if (patch.contractTermMonths() != null && !Objects.equals(patch.contractTermMonths(), p.getContractTermMonths())) {
            history.recordFieldEdit(HistoryEntityType.PLACEMENT, id, consultantId, recruiterId, "contractTermMonths",
                    p.getContractTermMonths().toString(), patch.contractTermMonths().toString(), actor);
            p.changeContractTermMonths(patch.contractTermMonths());
        }
        placements.flush();
        return toDetail(p, actor);
    }

    @Transactional(readOnly = true)
    public PlacementDetail getDetail(Long id, CurrentUser actor) {
        Placement p = placements.findDetailedById(id).orElseThrow();
        access.assertCanView(p, actor);
        return toDetail(p, actor);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlacementListItem> list(PlacementFilter filter, Pageable pageable, CurrentUser actor) {
        return PageResponse.from(placements.findAll(PlacementSpecifications.matching(filter, orgTime)
                .and(PlacementSpecifications.scopeFor(actor)), pageable), PlacementListItem::from);
    }

    /** Counts through the same filter and scope as {@link #list} (research R13). */
    @Transactional(readOnly = true)
    public long count(PlacementFilter filter, CurrentUser actor) {
        return placements.count(PlacementSpecifications.matching(filter, orgTime)
                .and(PlacementSpecifications.scopeFor(actor)));
    }

    /** The consultant detail Placements panel, limited to placements this viewer may see. */
    @Transactional(readOnly = true)
    public List<PlacementSummary> summariesFor(Long consultantId, CurrentUser viewer) {
        return placements.findByConsultant_IdOrderByCreatedAtDesc(consultantId).stream()
                .filter(p -> access.canView(p, viewer))
                .map(p -> new PlacementSummary(p.getId(), p.getClient().getName(), p.getStartDate(), p.expectedEndDate()))
                .toList();
    }

    private static void requireOffer(Submission s) {
        if (s.getStatus() != SubmissionStatus.OFFER) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_AT_OFFER,
                    "A placement can only be created from a submission at Offer.",
                    Map.of("submissionStatus", s.getStatus()));
        }
    }

    private static void checkStartDate(LocalDate startDate, Submission submission) {
        if (submission.getSubmittedDate() != null && startDate.isBefore(submission.getSubmittedDate())) {
            throw BusinessException.fieldError("startDate",
                    "Start date cannot be earlier than the submitted date (" + submission.getSubmittedDate() + ").");
        }
    }

    private PlacementDetail toDetail(Placement p, CurrentUser viewer) {
        String createdBy = p.getCreatedByUserId() == null ? null
                : users.findById(p.getCreatedByUserId()).map(AppUser::getDisplayName).orElse(null);
        var historyEntries = historyQuery.forEntity(HistoryEntityType.PLACEMENT, p.getId(), viewer, PageRequest.of(0, 100))
                .getContent();
        return new PlacementDetail(p.getId(), new PersonRef(p.getConsultant().getId(), p.getConsultant().getFullName()),
                new PersonRef(p.getRecruiter().getId(), p.getRecruiter().getFullName()),
                new NamedRef(p.getClient().getId(), p.getClient().getName()),
                new NamedRef(p.getVendor().getId(), p.getVendor().getName()), p.getStartDate(), p.getBillRate(),
                p.getContractTermMonths(), p.expectedEndDate(), p.getSubmission().getId(), p.getJobTitle(),
                p.getCreatedAt(), createdBy, historyEntries, p.getVersion());
    }
}
