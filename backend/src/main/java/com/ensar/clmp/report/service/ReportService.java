package com.ensar.clmp.report.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.common.time.OrgTime;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.placement.domain.Placement;
import com.ensar.clmp.placement.service.PlacementFilter;
import com.ensar.clmp.placement.service.PlacementSpecifications;
import com.ensar.clmp.report.web.BenchReadyReport;
import com.ensar.clmp.report.web.ConsultantPipelineReport;
import com.ensar.clmp.report.web.DateRangeParams;
import com.ensar.clmp.report.web.PlacementsByRecruiterReport;
import com.ensar.clmp.report.web.ReportLink;
import com.ensar.clmp.report.web.SubmissionsByRecruiterReport;
import com.ensar.clmp.report.web.VendorClientActivityReport;
import com.ensar.clmp.submission.domain.Submission;
import com.ensar.clmp.submission.domain.SubmissionStatus;
import com.ensar.clmp.submission.service.SubmissionFilter;
import com.ensar.clmp.submission.service.SubmissionSpecifications;

/**
 * The five FR-090 reports (ADMIN and MANAGER, organization-wide). Grouped queries apply the same
 * Specifications as the lists, and every figure carries the list query that reproduces it, so a
 * report total and its list always agree (research R13, FR-092).
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final EntityManager em;
    private final ConsultantRepository consultants;
    private final OrgTime orgTime;

    public ReportService(EntityManager em, ConsultantRepository consultants, OrgTime orgTime) {
        this.em = em;
        this.consultants = consultants;
        this.orgTime = orgTime;
    }

    public SubmissionsByRecruiterReport submissionsByRecruiter(DateRangeParams range) {
        SubmissionFilter filter = submittedIn(range);
        Map<Long, String> names = new TreeMap<>();
        Map<Long, Map<SubmissionStatus, Long>> counts = new LinkedHashMap<>();
        for (Tuple t : groupSubmissions(filter, "recruiter", "fullName")) {
            Long id = t.get(0, Long.class);
            names.put(id, t.get(1, String.class));
            counts.computeIfAbsent(id, k -> emptyStatusMap()).put(t.get(2, SubmissionStatus.class), t.get(3, Long.class));
        }
        String dates = "submittedFrom=" + range.from() + "&submittedTo=" + range.to();
        List<SubmissionsByRecruiterReport.Row> rows = new ArrayList<>();
        Map<SubmissionStatus, Long> totalsByStatus = emptyStatusMap();
        long grand = 0;
        for (Map.Entry<Long, String> e : sortByName(names)) {
            Map<SubmissionStatus, Long> byStatus = counts.get(e.getKey());
            long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
            Map<String, ReportLink> links = new LinkedHashMap<>();
            String base = "recruiterId=" + e.getKey() + "&" + dates;
            links.put("total", new ReportLink("submissions", base));
            byStatus.forEach((s, n) -> links.put(s.name(), new ReportLink("submissions", "status=" + s.name() + "&" + base)));
            rows.add(new SubmissionsByRecruiterReport.Row(e.getKey(), e.getValue(), total, byStatus, links));
            byStatus.forEach((s, n) -> totalsByStatus.merge(s, n, Long::sum));
            grand += total;
        }
        return new SubmissionsByRecruiterReport(range.from(), range.to(), rows,
                new SubmissionsByRecruiterReport.Totals(grand, totalsByStatus), rows.isEmpty());
    }

    public PlacementsByRecruiterReport placementsByRecruiter(DateRangeParams range) {
        PlacementFilter filter = PlacementFilter.createdBetween(range.from(), range.to());
        Map<Long, String> names = new TreeMap<>();
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Tuple t : groupPlacements(filter, "recruiter", "fullName")) {
            names.put(t.get(0, Long.class), t.get(1, String.class));
            counts.put(t.get(0, Long.class), t.get(2, Long.class));
        }
        String dates = "createdFrom=" + range.from() + "&createdTo=" + range.to();
        List<PlacementsByRecruiterReport.Row> rows = new ArrayList<>();
        long total = 0;
        for (Map.Entry<Long, String> e : sortByName(names)) {
            long n = counts.get(e.getKey());
            rows.add(new PlacementsByRecruiterReport.Row(e.getKey(), e.getValue(), n,
                    Map.of("placements", new ReportLink("placements", "recruiterId=" + e.getKey() + "&" + dates))));
            total += n;
        }
        return new PlacementsByRecruiterReport(range.from(), range.to(), rows,
                new PlacementsByRecruiterReport.Totals(total), rows.isEmpty());
    }

    public ConsultantPipelineReport consultantPipeline() {
        Map<ConsultantStatus, Long> counts = new EnumMap<>(ConsultantStatus.class);
        consultants.countByStatus().forEach(c -> counts.put(c.getStatus(), c.getCount()));
        List<ConsultantPipelineReport.Row> rows = new ArrayList<>();
        long total = 0;
        for (ConsultantStatus status : ConsultantStatus.values()) {
            long n = counts.getOrDefault(status, 0L);
            rows.add(new ConsultantPipelineReport.Row(status, n,
                    Map.of("count", new ReportLink("consultants", "status=" + status.name()))));
            total += n;
        }
        return new ConsultantPipelineReport(rows, total, total == 0);
    }

    public BenchReadyReport benchReady() {
        Map<ConsultantStatus, Long> counts = new EnumMap<>(ConsultantStatus.class);
        consultants.countByStatus().forEach(c -> counts.put(c.getStatus(), c.getCount()));
        long bench = counts.getOrDefault(ConsultantStatus.BENCH, 0L);
        long ready = counts.getOrDefault(ConsultantStatus.READY, 0L);
        Map<String, ReportLink> links = new LinkedHashMap<>();
        links.put("bench", new ReportLink("consultants", "status=BENCH"));
        links.put("ready", new ReportLink("consultants", "status=READY"));
        return new BenchReadyReport(bench, ready, orgTime.today(), links, bench + ready == 0);
    }

    public VendorClientActivityReport vendorClientActivity(DateRangeParams range) {
        List<VendorClientActivityReport.Row> vendors = activity("vendor", "vendorId", range);
        List<VendorClientActivityReport.Row> clients = activity("client", "clientId", range);
        return new VendorClientActivityReport(range.from(), range.to(), vendors, clients,
                vendors.isEmpty() && clients.isEmpty());
    }

    private List<VendorClientActivityReport.Row> activity(String association, String param, DateRangeParams range) {
        Map<Long, String> names = new TreeMap<>();
        Map<Long, long[]> counts = new LinkedHashMap<>(); // [submissions, interviewsScheduled, placements]
        for (Tuple t : groupSubmissions(submittedIn(range), association, "name")) {
            Long id = t.get(0, Long.class);
            names.put(id, t.get(1, String.class));
            long[] c = counts.computeIfAbsent(id, k -> new long[3]);
            long n = t.get(3, Long.class);
            c[0] += n;
            if (t.get(2, SubmissionStatus.class) == SubmissionStatus.INTERVIEW_SCHEDULED) {
                c[1] += n;
            }
        }
        for (Tuple t : groupPlacements(PlacementFilter.createdBetween(range.from(), range.to()), association, "name")) {
            Long id = t.get(0, Long.class);
            names.put(id, t.get(1, String.class));
            counts.computeIfAbsent(id, k -> new long[3])[2] += t.get(2, Long.class);
        }
        String submitted = "submittedFrom=" + range.from() + "&submittedTo=" + range.to();
        String created = "createdFrom=" + range.from() + "&createdTo=" + range.to();
        List<VendorClientActivityReport.Row> rows = new ArrayList<>();
        for (Map.Entry<Long, String> e : sortByName(names)) {
            long[] c = counts.get(e.getKey());
            String id = param + "=" + e.getKey();
            Map<String, ReportLink> links = new LinkedHashMap<>();
            links.put("submissions", new ReportLink("submissions", id + "&" + submitted));
            links.put("interviewsScheduled", new ReportLink("submissions", "status=INTERVIEW_SCHEDULED&" + id + "&" + submitted));
            links.put("placements", new ReportLink("placements", id + "&" + created));
            rows.add(new VendorClientActivityReport.Row(e.getKey(), e.getValue(), c[0], c[1], c[2], links));
        }
        return rows;
    }

    /** (association id, association label, status, count) through the submission list's filter. */
    private List<Tuple> groupSubmissions(SubmissionFilter filter, String association, String labelField) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Submission> root = q.from(Submission.class);
        Specification<Submission> spec = SubmissionSpecifications.matching(filter);
        Path<Object> group = root.get(association);
        q.multiselect(group.get("id"), group.get(labelField), root.get("status"), cb.count(root))
                .where(spec.toPredicate(root, q, cb))
                .groupBy(group.get("id"), group.get(labelField), root.get("status"));
        return em.createQuery(q).getResultList();
    }

    /** (association id, association label, count) through the placement list's filter. */
    private List<Tuple> groupPlacements(PlacementFilter filter, String association, String labelField) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Placement> root = q.from(Placement.class);
        Specification<Placement> spec = PlacementSpecifications.matching(filter, orgTime);
        Path<Object> group = root.get(association);
        q.multiselect(group.get("id"), group.get(labelField), cb.count(root))
                .where(spec.toPredicate(root, q, cb))
                .groupBy(group.get("id"), group.get(labelField));
        return em.createQuery(q).getResultList();
    }

    private static SubmissionFilter submittedIn(DateRangeParams range) {
        return new SubmissionFilter(null, null, null, null, null, range.from(), range.to());
    }

    private static Map<SubmissionStatus, Long> emptyStatusMap() {
        Map<SubmissionStatus, Long> map = new EnumMap<>(SubmissionStatus.class);
        for (SubmissionStatus s : SubmissionStatus.values()) {
            if (s != SubmissionStatus.DRAFT) {
                map.put(s, 0L);
            }
        }
        return map;
    }

    private static List<Map.Entry<Long, String>> sortByName(Map<Long, String> names) {
        return names.entrySet().stream().sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER)).toList();
    }
}
