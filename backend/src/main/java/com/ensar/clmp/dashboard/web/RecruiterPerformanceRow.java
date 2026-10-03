package com.ensar.clmp.dashboard.web;

public record RecruiterPerformanceRow(Long recruiterId, String recruiterName, long assignedConsultants,
        long activeSubmissions, long interviews, long placementsThisMonth) {
}
