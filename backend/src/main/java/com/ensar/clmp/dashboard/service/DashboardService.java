package com.ensar.clmp.dashboard.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.common.domain.GroupCount;
import com.ensar.clmp.common.time.OrgTime;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.consultant.service.ConsultantFilter;
import com.ensar.clmp.consultant.service.ConsultantService;
import com.ensar.clmp.dashboard.web.CountTile;
import com.ensar.clmp.dashboard.web.DashboardResponse;
import com.ensar.clmp.dashboard.web.RecruiterPerformanceRow;
import com.ensar.clmp.dashboard.web.StatusCount;
import com.ensar.clmp.history.service.HistoryQueryService;
import com.ensar.clmp.placement.domain.PlacementRepository;
import com.ensar.clmp.placement.service.PlacementFilter;
import com.ensar.clmp.placement.service.PlacementService;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;
import com.ensar.clmp.submission.domain.SubmissionRepository;
import com.ensar.clmp.submission.domain.SubmissionStatus;
import com.ensar.clmp.submission.service.SubmissionFilter;
import com.ensar.clmp.submission.service.SubmissionService;

/**
 * Role-shaped dashboard (FR-080–FR-083). Every figure is counted through the same filter and
 * caller scope as the list it links to, so a figure and its list always agree (research R13).
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    static final String UNLINKED_MESSAGE = "Your account is not linked to a recruiter profile. Contact an Admin.";
    private static final int ACTIVITY_LIMIT = 20;

    private final ConsultantService consultants;
    private final SubmissionService submissions;
    private final PlacementService placements;
    private final HistoryQueryService history;
    private final RecruiterRepository recruiterRepository;
    private final ConsultantRepository consultantRepository;
    private final SubmissionRepository submissionRepository;
    private final PlacementRepository placementRepository;
    private final OrgTime orgTime;

    public DashboardService(ConsultantService consultants, SubmissionService submissions, PlacementService placements,
            HistoryQueryService history, RecruiterRepository recruiterRepository,
            ConsultantRepository consultantRepository, SubmissionRepository submissionRepository,
            PlacementRepository placementRepository, OrgTime orgTime) {
        this.consultants = consultants;
        this.submissions = submissions;
        this.placements = placements;
        this.history = history;
        this.recruiterRepository = recruiterRepository;
        this.consultantRepository = consultantRepository;
        this.submissionRepository = submissionRepository;
        this.placementRepository = placementRepository;
        this.orgTime = orgTime;
    }

    public DashboardResponse dashboard(CurrentUser user) {
        return switch (user.role()) {
            case ADMIN, MANAGER -> new DashboardResponse(DashboardResponse.Scope.ORGANIZATION, null,
                    withNeedsReassignment(coreCounts(user), user), null, recruiterPerformance(),
                    history.recentActivity(user, ACTIVITY_LIMIT));
            case RECRUITER -> new DashboardResponse(DashboardResponse.Scope.OWN,
                    user.recruiterId() == null ? UNLINKED_MESSAGE : null, coreCounts(user), null, null,
                    history.recentActivity(user, ACTIVITY_LIMIT));
            case HR_OPERATIONS -> new DashboardResponse(DashboardResponse.Scope.CONSULTANT_PIPELINE, null,
                    List.of(consultantTile("BENCH_CONSULTANTS", "Bench", ConsultantStatus.BENCH, user),
                            consultantTile("READY_CONSULTANTS", "Ready", ConsultantStatus.READY, user)),
                    consultantsByStatus(user), null, history.recentActivity(user, ACTIVITY_LIMIT));
        };
    }

    /** The five FR-080 figures within the caller's scope. */
    private List<CountTile> coreCounts(CurrentUser user) {
        LocalDate monthStart = orgTime.currentMonthStart();
        LocalDate monthEnd = orgTime.currentMonthEnd();
        List<CountTile> tiles = new ArrayList<>();
        tiles.add(consultantTile("BENCH_CONSULTANTS", "Bench", ConsultantStatus.BENCH, user));
        tiles.add(consultantTile("READY_CONSULTANTS", "Ready", ConsultantStatus.READY, user));
        tiles.add(new CountTile("ACTIVE_SUBMISSIONS", "Active submissions",
                submissions.count(SubmissionFilter.byStatuses(SubmissionStatus.ACTIVE), user),
                new CountTile.Link("submissions", statusQuery(SubmissionStatus.ACTIVE))));
        tiles.add(new CountTile("INTERVIEW_SUBMISSIONS", "In interviews",
                submissions.count(SubmissionFilter.byStatuses(SubmissionStatus.INTERVIEW_STAGE), user),
                new CountTile.Link("submissions", statusQuery(SubmissionStatus.INTERVIEW_STAGE))));
        tiles.add(new CountTile("PLACEMENTS_THIS_MONTH", "Placements this month",
                placements.count(PlacementFilter.createdBetween(monthStart, monthEnd), user),
                new CountTile.Link("placements", "createdFrom=" + monthStart + "&createdTo=" + monthEnd)));
        return tiles;
    }

    private List<CountTile> withNeedsReassignment(List<CountTile> tiles, CurrentUser user) {
        ConsultantFilter flagged = new ConsultantFilter(null, null, null, null, null, true);
        tiles.add(new CountTile("NEEDS_REASSIGNMENT", "Needs reassignment", consultants.count(flagged, user),
                new CountTile.Link("consultants", "needsReassignment=true")));
        return tiles;
    }

    private CountTile consultantTile(String key, String label, ConsultantStatus status, CurrentUser user) {
        return new CountTile(key, label, consultants.count(ConsultantFilter.byStatus(status), user),
                new CountTile.Link("consultants", "status=" + status.name()));
    }

    /** HR pipeline: every status, zeros included, each linking to the consultant list. */
    private List<StatusCount> consultantsByStatus(CurrentUser user) {
        List<StatusCount> rows = new ArrayList<>();
        for (ConsultantStatus status : ConsultantStatus.values()) {
            rows.add(new StatusCount(status, consultants.count(ConsultantFilter.byStatus(status), user),
                    new CountTile.Link("consultants", "status=" + status.name())));
        }
        return rows;
    }

    /** Per active recruiter, using grouped count queries only (research R14). */
    private List<RecruiterPerformanceRow> recruiterPerformance() {
        List<Recruiter> active = recruiterRepository.findAll(Sort.by("fullName")).stream()
                .filter(r -> r.getStatus() == RecruiterStatus.ACTIVE).toList();
        List<Long> ids = active.stream().map(Recruiter::getId).toList();
        Map<Long, Long> assigned = ids.isEmpty() ? Map.of()
                : consultantRepository.countByCurrentRecruiterIdIn(ids).stream().collect(Collectors.toMap(
                        ConsultantRepository.RecruiterCount::getRecruiterId, ConsultantRepository.RecruiterCount::getCount));
        Map<Long, Long> activeSubs = toMap(submissionRepository.countByRecruiterWithStatusIn(SubmissionStatus.ACTIVE));
        Map<Long, Long> interviews = toMap(
                submissionRepository.countByRecruiterWithStatusIn(SubmissionStatus.INTERVIEW_STAGE));
        Map<Long, Long> placed = toMap(placementRepository.countByRecruiterCreatedBetween(
                orgTime.startOf(orgTime.currentMonthStart()), orgTime.endOf(orgTime.currentMonthEnd())));
        return active.stream().map(r -> new RecruiterPerformanceRow(r.getId(), r.getFullName(),
                assigned.getOrDefault(r.getId(), 0L), activeSubs.getOrDefault(r.getId(), 0L),
                interviews.getOrDefault(r.getId(), 0L), placed.getOrDefault(r.getId(), 0L))).toList();
    }

    private static Map<Long, Long> toMap(List<GroupCount> counts) {
        return counts.stream().collect(Collectors.toMap(GroupCount::getGroupId, GroupCount::getCount));
    }

    static String statusQuery(Collection<SubmissionStatus> statuses) {
        return statuses.stream().sorted().map(s -> "status=" + s.name()).collect(Collectors.joining("&"));
    }
}
