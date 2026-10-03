package com.ensar.clmp.marketing.domain;

import java.util.Set;

/** FR-041. */
public enum MarketingStatus {
    DRAFT, ACTIVE, HOLD, CLOSED;

    /** At most one assignment per consultant may be in these statuses (FR-042). */
    public static final Set<MarketingStatus> OPEN = Set.of(DRAFT, ACTIVE, HOLD);
}
