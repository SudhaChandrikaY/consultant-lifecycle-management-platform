package com.ensar.clmp.marketing.web;

import java.time.LocalDate;

import com.ensar.clmp.marketing.domain.MarketingStatus;

/** The consultant detail page's Marketing panel (not shown to HR_OPERATIONS). */
public record MarketingSummary(Long id, MarketingStatus status, LocalDate targetDate, boolean overdue) {
}
