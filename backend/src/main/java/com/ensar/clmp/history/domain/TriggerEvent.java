package com.ensar.clmp.history.domain;

/** The workflow event behind an automatic (system-triggered) change (FR-032, FR-036). */
public enum TriggerEvent {
    MARKETING_ACTIVATED,
    MARKETING_REOPENED,
    MARKETING_CLOSED,
    SUBMISSION_INTERVIEW_SCHEDULED,
    SUBMISSION_LEFT_INTERVIEW_STAGES,
    PLACEMENT_CREATED,
    CONSULTANT_HOLD,
    CONSULTANT_INACTIVE
}
