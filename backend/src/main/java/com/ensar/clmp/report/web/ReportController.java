package com.ensar.clmp.report.web;

import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ensar.clmp.common.time.OrgTime;
import com.ensar.clmp.report.service.ReportService;

/** On-screen reports for ADMIN and MANAGER (FR-090–FR-093). */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ReportController {

    private final ReportService reports;
    private final OrgTime orgTime;

    public ReportController(ReportService reports, OrgTime orgTime) {
        this.reports = reports;
        this.orgTime = orgTime;
    }

    @GetMapping("/submissions-by-recruiter")
    public SubmissionsByRecruiterReport submissionsByRecruiter(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return reports.submissionsByRecruiter(DateRangeParams.resolve(from, to, orgTime));
    }

    @GetMapping("/placements-by-recruiter")
    public PlacementsByRecruiterReport placementsByRecruiter(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return reports.placementsByRecruiter(DateRangeParams.resolve(from, to, orgTime));
    }

    @GetMapping("/consultant-pipeline")
    public ConsultantPipelineReport consultantPipeline() {
        return reports.consultantPipeline();
    }

    @GetMapping("/bench-ready")
    public BenchReadyReport benchReady() {
        return reports.benchReady();
    }

    @GetMapping("/vendor-client-activity")
    public VendorClientActivityReport vendorClientActivity(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return reports.vendorClientActivity(DateRangeParams.resolve(from, to, orgTime));
    }
}
