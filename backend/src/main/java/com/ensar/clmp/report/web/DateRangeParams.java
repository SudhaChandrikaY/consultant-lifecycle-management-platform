package com.ensar.clmp.report.web;

import java.time.LocalDate;

import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.time.OrgTime;

/** Report date range: defaults to the current calendar month (FR-091); {@code from} must not be after {@code to}. */
public record DateRangeParams(LocalDate from, LocalDate to) {

    public static DateRangeParams resolve(LocalDate from, LocalDate to, OrgTime orgTime) {
        LocalDate f = from == null ? orgTime.currentMonthStart() : from;
        LocalDate t = to == null ? (from == null ? orgTime.currentMonthEnd() : f.withDayOfMonth(f.lengthOfMonth())) : to;
        if (f.isAfter(t)) {
            throw BusinessException.fieldError("to", "The end date must be on or after the start date.");
        }
        return new DateRangeParams(f, t);
    }
}
