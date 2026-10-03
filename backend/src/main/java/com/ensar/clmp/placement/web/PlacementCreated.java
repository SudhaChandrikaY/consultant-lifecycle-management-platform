package com.ensar.clmp.placement.web;

import java.util.List;

import com.ensar.clmp.submission.domain.SubmissionStatus;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * The created placement plus the consultant's other open submissions, which the UI offers to
 * withdraw; none is changed automatically (FR-076).
 */
public record PlacementCreated(@JsonUnwrapped PlacementDetail placement, List<OtherOpenSubmission> otherOpenSubmissions) {

    public record OtherOpenSubmission(Long id, String vendorName, String clientName, String jobTitle,
            SubmissionStatus status, Long version) {
    }
}
