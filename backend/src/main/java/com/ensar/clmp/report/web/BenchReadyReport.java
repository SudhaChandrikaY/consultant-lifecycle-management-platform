package com.ensar.clmp.report.web;

import java.time.LocalDate;
import java.util.Map;

public record BenchReadyReport(long bench, long ready, LocalDate asOf, Map<String, ReportLink> links, boolean empty) {
}
