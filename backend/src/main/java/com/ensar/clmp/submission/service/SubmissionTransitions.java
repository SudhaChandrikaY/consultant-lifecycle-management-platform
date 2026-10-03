package com.ensar.clmp.submission.service;

import static com.ensar.clmp.submission.domain.SubmissionStatus.DRAFT;
import static com.ensar.clmp.submission.domain.SubmissionStatus.INTERVIEW_CLEARED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.INTERVIEW_SCHEDULED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.OFFER;
import static com.ensar.clmp.submission.domain.SubmissionStatus.REJECTED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.SUBMITTED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.UNDER_REVIEW;
import static com.ensar.clmp.submission.domain.SubmissionStatus.WITHDRAWN;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.submission.domain.SubmissionStatus;

/**
 * FR-058 manual transitions. OFFER → PLACED is deliberately absent: it happens only by creating a
 * placement (FR-070). Terminal statuses have no outgoing edges (FR-057).
 */
@Component
public class SubmissionTransitions {

    private static final Map<SubmissionStatus, List<SubmissionStatus>> EDGES = Map.of(
            DRAFT, List.of(SUBMITTED, WITHDRAWN),
            SUBMITTED, List.of(UNDER_REVIEW, INTERVIEW_SCHEDULED, REJECTED, WITHDRAWN),
            UNDER_REVIEW, List.of(INTERVIEW_SCHEDULED, REJECTED, WITHDRAWN),
            INTERVIEW_SCHEDULED, List.of(INTERVIEW_CLEARED, REJECTED, WITHDRAWN),
            INTERVIEW_CLEARED, List.of(INTERVIEW_SCHEDULED, OFFER, REJECTED, WITHDRAWN),
            OFFER, List.of(REJECTED, WITHDRAWN));

    public List<SubmissionStatus> allowedFrom(SubmissionStatus from) {
        return EDGES.getOrDefault(from, List.of());
    }

    public void validate(SubmissionStatus from, SubmissionStatus to) {
        List<SubmissionStatus> allowed = allowedFrom(from);
        if (!allowed.contains(to)) {
            String detail = to == SubmissionStatus.PLACED
                    ? "A submission becomes Placed only by creating a placement."
                    : "Cannot change a " + from + " submission to " + to + ".";
            throw new BusinessException(ErrorCode.INVALID_TRANSITION, detail,
                    Map.of("currentStatus", from, "allowedTransitions", allowed));
        }
    }
}
