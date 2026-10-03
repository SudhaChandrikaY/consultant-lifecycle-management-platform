package com.ensar.clmp.report.web;

import java.util.List;
import java.util.Map;

import com.ensar.clmp.consultant.domain.ConsultantStatus;

/** Current consultants per status, all eight statuses including zeros (AS 8.3). */
public record ConsultantPipelineReport(List<Row> rows, long total, boolean empty) {

    public record Row(ConsultantStatus status, long count, Map<String, ReportLink> links) {
    }
}
