package com.ensar.clmp.placement.web;

import java.time.LocalDate;

/** A row in the consultant detail page's Placements panel (never sent to HR_OPERATIONS). */
public record PlacementSummary(Long id, String clientName, LocalDate startDate, LocalDate expectedEndDate) {
}
