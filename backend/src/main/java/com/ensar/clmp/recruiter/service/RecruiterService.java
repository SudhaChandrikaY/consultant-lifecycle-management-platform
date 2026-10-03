package com.ensar.clmp.recruiter.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.AppUser;
import com.ensar.clmp.auth.domain.AppUserRepository;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.common.error.FieldErrorItem;
import com.ensar.clmp.common.service.VersionGuard;
import com.ensar.clmp.common.web.PageResponse;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;
import com.ensar.clmp.recruiter.web.LinkableUser;
import com.ensar.clmp.recruiter.web.RecruiterDetail;
import com.ensar.clmp.recruiter.web.RecruiterListItem;
import com.ensar.clmp.recruiter.web.RecruiterRequest;
import com.ensar.clmp.reference.domain.Region;
import com.ensar.clmp.reference.domain.RegionRepository;
import com.ensar.clmp.reference.domain.Team;
import com.ensar.clmp.reference.domain.TeamRepository;

/** Recruiter roster (FR-010–FR-016). Deactivation never touches marketing or submissions (FR-014). */
@Service
public class RecruiterService {

    private final RecruiterRepository recruiters;
    private final ConsultantRepository consultants;
    private final TeamRepository teams;
    private final RegionRepository regions;
    private final AppUserRepository users;
    private final HistoryService history;
    private final VersionGuard versionGuard;
    private final Clock clock;

    public RecruiterService(RecruiterRepository recruiters, ConsultantRepository consultants, TeamRepository teams,
            RegionRepository regions, AppUserRepository users, HistoryService history, VersionGuard versionGuard,
            Clock clock) {
        this.recruiters = recruiters;
        this.consultants = consultants;
        this.teams = teams;
        this.regions = regions;
        this.users = users;
        this.history = history;
        this.versionGuard = versionGuard;
        this.clock = clock;
    }

    @Transactional
    public RecruiterDetail create(RecruiterRequest request, CurrentUser actor) {
        assertAdmin(actor);
        if (recruiters.existsByEmailIgnoreCase(request.email().trim())) {
            throw duplicateEmail();
        }
        Team team = activeTeam(request.teamId());
        Region region = activeRegion(request.regionId());
        AppUser user = linkableUser(request.linkedUserId(), null);
        Recruiter recruiter = recruiters.saveAndFlush(new Recruiter(request.fullName().trim(), request.email(),
                blankToNull(request.phone()), team, region, user, clock.instant()));
        history.recordCreated(HistoryEntityType.RECRUITER, recruiter.getId(), null, recruiter.getId(),
                RecruiterStatus.ACTIVE.name(), actor);
        return toDetail(recruiter, null);
    }

    @Transactional
    public RecruiterDetail update(Long id, RecruiterRequest request, CurrentUser actor) {
        assertAdmin(actor);
        Recruiter recruiter = recruiters.findById(id).orElseThrow();
        versionGuard.check(recruiter, request.version());
        if (recruiters.existsByEmailIgnoreCaseAndIdNot(request.email().trim(), id)) {
            throw duplicateEmail();
        }
        Team team = recruiter.getTeam().getId().equals(request.teamId()) ? recruiter.getTeam()
                : activeTeam(request.teamId());
        Region region = recruiter.getRegion().getId().equals(request.regionId()) ? recruiter.getRegion()
                : activeRegion(request.regionId());
        AppUser user = linkableUser(request.linkedUserId(), recruiter);
        recruiter.update(request.fullName().trim(), request.email(), blankToNull(request.phone()), team, region, user,
                clock.instant());
        recruiters.flush();
        return toDetail(recruiter, null);
    }

    /**
     * Activates or deactivates. Deactivating a recruiter who still has consultants needs
     * {@code confirm=true} (FR-014); those consultants then show "needs reassignment".
     */
    @Transactional
    public RecruiterDetail changeStatus(Long id, RecruiterStatus status, Boolean confirm, Long version,
            CurrentUser actor) {
        assertAdmin(actor);
        Recruiter recruiter = recruiters.findById(id).orElseThrow();
        versionGuard.check(recruiter, version);
        if (recruiter.getStatus() == status) {
            return toDetail(recruiter, null);
        }
        Long flagged = null;
        if (status == RecruiterStatus.INACTIVE) {
            long assigned = consultants.countByCurrentRecruiter_Id(id);
            if (assigned > 0 && !Boolean.TRUE.equals(confirm)) {
                throw new BusinessException(ErrorCode.CONFIRMATION_REQUIRED,
                        assigned + " consultant(s) are still assigned and will be flagged for reassignment.",
                        Map.of("affectedConsultantCount", assigned));
            }
            flagged = assigned;
        }
        RecruiterStatus old = recruiter.getStatus();
        recruiter.changeStatus(status, clock.instant());
        history.recordStatusChange(HistoryEntityType.RECRUITER, id, null, id, old.name(), status.name(), null, null,
                actor);
        recruiters.flush();
        return toDetail(recruiter, flagged);
    }

    @Transactional(readOnly = true)
    public RecruiterDetail getDetail(Long id) {
        return toDetail(recruiters.findById(id).orElseThrow(), null);
    }

    /** One grouped count query fills assignedConsultantCount for the whole page (research R14). */
    @Transactional(readOnly = true)
    public PageResponse<RecruiterListItem> list(RecruiterFilter filter, Pageable pageable) {
        Page<Recruiter> page = recruiters.findAll(RecruiterSpecifications.matching(filter), pageable);
        Map<Long, Long> counts = countsFor(page.getContent().stream().map(Recruiter::getId).toList());
        return PageResponse.from(page, r -> RecruiterListItem.from(r, counts.getOrDefault(r.getId(), 0L)));
    }

    /** RECRUITER users not yet linked to a recruiter profile (FR-015). */
    @Transactional(readOnly = true)
    public List<LinkableUser> linkableUsers(CurrentUser actor) {
        assertAdmin(actor);
        return users.findByRoleOrderByDisplayName(Role.RECRUITER).stream()
                .filter(AppUser::isActive)
                .filter(u -> !recruiters.existsByUserId(u.getId()))
                .map(u -> new LinkableUser(u.getId(), u.getUsername(), u.getDisplayName()))
                .toList();
    }

    private Map<Long, Long> countsFor(List<Long> recruiterIds) {
        if (recruiterIds.isEmpty()) {
            return Map.of();
        }
        return consultants.countByCurrentRecruiterIdIn(recruiterIds).stream()
                .collect(Collectors.toMap(ConsultantRepository.RecruiterCount::getRecruiterId,
                        ConsultantRepository.RecruiterCount::getCount));
    }

    private RecruiterDetail toDetail(Recruiter r, Long flagged) {
        long count = consultants.countByCurrentRecruiter_Id(r.getId());
        RecruiterListItem item = RecruiterListItem.from(r, count);
        LinkableUser linked = r.getUser() == null ? null
                : new LinkableUser(r.getUser().getId(), r.getUser().getUsername(), r.getUser().getDisplayName());
        return new RecruiterDetail(item.id(), item.fullName(), item.team(), item.region(), item.status(),
                item.assignedConsultantCount(), r.getEmail(), r.getPhone(), linked, r.getVersion(), flagged);
    }

    private Team activeTeam(Long id) {
        return teams.findById(id).filter(Team::isActive)
                .orElseThrow(() -> BusinessException.fieldError("teamId", "Choose an active team."));
    }

    private Region activeRegion(Long id) {
        return regions.findById(id).filter(Region::isActive)
                .orElseThrow(() -> BusinessException.fieldError("regionId", "Choose an active region."));
    }

    /** The linked user must be a RECRUITER account not linked to another recruiter (FR-015). */
    private AppUser linkableUser(Long userId, Recruiter current) {
        if (userId == null) {
            return null;
        }
        if (current != null && current.getUser() != null && current.getUser().getId().equals(userId)) {
            return current.getUser();
        }
        AppUser user = users.findById(userId).orElseThrow(
                () -> BusinessException.fieldError("linkedUserId", "The selected user does not exist."));
        if (user.getRole() != Role.RECRUITER) {
            throw BusinessException.fieldError("linkedUserId", "Only RECRUITER users can be linked.");
        }
        if (recruiters.existsByUserId(userId)) {
            throw BusinessException.fieldError("linkedUserId", "That user is already linked to a recruiter.");
        }
        return user;
    }

    private static void assertAdmin(CurrentUser actor) {
        if (!actor.is(Role.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN maintains recruiters");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BusinessException duplicateEmail() {
        return new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email is already in use.", Map.of(),
                List.of(new FieldErrorItem("email", "Email is already in use by another recruiter.")));
    }
}
