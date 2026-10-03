package com.ensar.clmp.marketing;

import static com.ensar.clmp.marketing.domain.MarketingStatus.ACTIVE;
import static com.ensar.clmp.marketing.domain.MarketingStatus.CLOSED;
import static com.ensar.clmp.marketing.domain.MarketingStatus.DRAFT;
import static com.ensar.clmp.marketing.domain.MarketingStatus.HOLD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.access.AccessDeniedException;

import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.marketing.domain.MarketingAssignment;
import com.ensar.clmp.marketing.domain.MarketingStatus;
import com.ensar.clmp.marketing.service.MarketingTransitions;

/** FR-041 marketing transition table per role. */
class MarketingTransitionsTest {

    private final MarketingTransitions transitions = new MarketingTransitions();

    private void ok(MarketingStatus from, MarketingStatus to, String closeReason, String reason, Role role,
            boolean owner) {
        assertThatCode(() -> transitions.validate(from, to, closeReason, reason, role, owner))
                .doesNotThrowAnyException();
    }

    private void denied(MarketingStatus from, MarketingStatus to, Role role, boolean owner) {
        assertThatThrownBy(() -> transitions.validate(from, to, null, "reason", role, owner))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void draftToActiveIsAdminOrOwningRecruiterOnly() {
        ok(DRAFT, ACTIVE, null, null, Role.ADMIN, false);
        ok(DRAFT, ACTIVE, null, null, Role.RECRUITER, true);
        denied(DRAFT, ACTIVE, Role.MANAGER, false);
        denied(DRAFT, ACTIVE, Role.RECRUITER, false);
        denied(DRAFT, ACTIVE, Role.HR_OPERATIONS, false);
    }

    @Test
    void draftToClosedNeedsNoReason() {
        ok(DRAFT, CLOSED, null, null, Role.ADMIN, false);
        ok(DRAFT, CLOSED, null, null, Role.RECRUITER, true);
        ok(DRAFT, CLOSED, null, "", Role.MANAGER, false);
    }

    @ParameterizedTest
    @CsvSource({ "ADMIN,false", "RECRUITER,true", "MANAGER,false" })
    void holdReopenAndCloseAreOpenToAdminOwnerAndManager(Role role, boolean owner) {
        ok(ACTIVE, HOLD, null, "Client freeze", role, owner);
        ok(HOLD, ACTIVE, null, null, role, owner);
        ok(ACTIVE, CLOSED, null, "Done", role, owner);
        ok(HOLD, CLOSED, null, "Done", role, owner);
    }

    @ParameterizedTest
    @CsvSource({ "ACTIVE,HOLD", "ACTIVE,CLOSED", "HOLD,CLOSED" })
    void reasonRequired(MarketingStatus from, MarketingStatus to) {
        for (String blank : new String[] { null, "", "   " }) {
            assertThatThrownBy(() -> transitions.validate(from, to, null, blank, Role.ADMIN, false))
                    .isInstanceOfSatisfying(BusinessException.class, e -> {
                        assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                        assertThat(e.getFieldErrors()).extracting("field").containsExactly("reason");
                    });
        }
    }

    @Test
    void closedToActiveIsAdminOrManagerOnly() {
        ok(CLOSED, ACTIVE, "Paused", null, Role.ADMIN, false);
        ok(CLOSED, ACTIVE, "Paused", null, Role.MANAGER, false);
        denied(CLOSED, ACTIVE, Role.RECRUITER, true);
    }

    @Test
    void closedWithPlacedCannotBeReopened() {
        assertThatThrownBy(() -> transitions.validate(CLOSED, ACTIVE, MarketingAssignment.PLACED_REASON, null,
                Role.ADMIN, false))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.BUSINESS_RULE));
        assertThat(transitions.allowedFor(CLOSED, MarketingAssignment.PLACED_REASON, Role.ADMIN, false)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "DRAFT,HOLD", "DRAFT,DRAFT", "ACTIVE,DRAFT", "HOLD,DRAFT", "HOLD,HOLD", "CLOSED,HOLD",
            "CLOSED,DRAFT", "CLOSED,CLOSED", "ACTIVE,ACTIVE" })
    void invalidTransitionsAreRefused(MarketingStatus from, MarketingStatus to) {
        assertThatThrownBy(() -> transitions.validate(from, to, null, "r", Role.ADMIN, false))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_TRANSITION);
                    assertThat(e.getExtras()).containsKeys("currentStatus", "allowedTransitions");
                });
    }

    @Test
    void allowedForReflectsRole() {
        assertThat(transitions.allowedFor(DRAFT, null, Role.ADMIN, false)).containsExactly(ACTIVE, CLOSED);
        assertThat(transitions.allowedFor(DRAFT, null, Role.MANAGER, false)).containsExactly(CLOSED);
        assertThat(transitions.allowedFor(DRAFT, null, Role.RECRUITER, true)).containsExactly(ACTIVE, CLOSED);
        assertThat(transitions.allowedFor(DRAFT, null, Role.RECRUITER, false)).isEmpty();
        assertThat(transitions.allowedFor(ACTIVE, null, Role.MANAGER, false)).containsExactly(HOLD, CLOSED);
        assertThat(transitions.allowedFor(HOLD, null, Role.RECRUITER, true)).containsExactly(ACTIVE, CLOSED);
        assertThat(transitions.allowedFor(CLOSED, "Paused", Role.RECRUITER, true)).isEmpty();
        assertThat(transitions.allowedFor(CLOSED, "Paused", Role.MANAGER, false)).containsExactly(ACTIVE);
        assertThat(transitions.allowedFor(ACTIVE, null, Role.HR_OPERATIONS, false)).isEmpty();
    }
}
