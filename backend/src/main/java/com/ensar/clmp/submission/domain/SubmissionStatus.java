package com.ensar.clmp.submission.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** FR-056 statuses and the groupings used by the dashboard, reports, and lifecycle rules. */
public enum SubmissionStatus {
    DRAFT, SUBMITTED, UNDER_REVIEW, INTERVIEW_SCHEDULED, INTERVIEW_CLEARED, REJECTED, OFFER, PLACED, WITHDRAWN;

    /** FR-080 "active submissions". */
    public static final Set<SubmissionStatus> ACTIVE = Collections.unmodifiableSet(
            EnumSet.of(SUBMITTED, UNDER_REVIEW, INTERVIEW_SCHEDULED, INTERVIEW_CLEARED, OFFER));
    public static final Set<SubmissionStatus> INTERVIEW_STAGE = Collections.unmodifiableSet(
            EnumSet.of(INTERVIEW_SCHEDULED, INTERVIEW_CLEARED));
    /** Keeps a consultant Interviewing (FR-032). */
    public static final Set<SubmissionStatus> INTERVIEW_OR_OFFER = Collections.unmodifiableSet(
            EnumSet.of(INTERVIEW_SCHEDULED, INTERVIEW_CLEARED, OFFER));
    /** FR-057. */
    public static final Set<SubmissionStatus> TERMINAL = Collections.unmodifiableSet(
            EnumSet.of(REJECTED, WITHDRAWN, PLACED));
    /** Everything not terminal. */
    public static final Set<SubmissionStatus> OPEN = Collections.unmodifiableSet(EnumSet.complementOf(
            EnumSet.of(REJECTED, WITHDRAWN, PLACED)));
}
