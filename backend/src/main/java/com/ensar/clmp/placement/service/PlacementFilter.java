package com.ensar.clmp.placement.service;

import java.time.LocalDate;

/** Placement list filter; created dates are org-zone calendar days (dashboard "this month"). */
public record PlacementFilter(Long recruiterId, Long clientId, Long vendorId, LocalDate startFrom, LocalDate startTo,
        LocalDate createdFrom, LocalDate createdTo) {

    public static PlacementFilter createdBetween(LocalDate from, LocalDate to) {
        return new PlacementFilter(null, null, null, null, null, from, to);
    }
}
