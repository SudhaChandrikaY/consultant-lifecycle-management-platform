package com.ensar.clmp.lifecycle;

import static com.ensar.clmp.consultant.domain.ConsultantStatus.ACTIVE_PROJECT;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.BENCH;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.HOLD;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.INACTIVE;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.INTERVIEWING;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.MARKETING;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.PLACED;
import static com.ensar.clmp.consultant.domain.ConsultantStatus.READY;
import static com.ensar.clmp.support.ConsultantFixtures.completeProfile;
import static com.ensar.clmp.support.ConsultantFixtures.consultant;
import static com.ensar.clmp.support.ConsultantFixtures.minimalProfile;
import static com.ensar.clmp.support.ConsultantFixtures.recruiter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.common.service.VersionGuard;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.consultant.service.ReadinessChecker;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;
import com.ensar.clmp.support.ConsultantFixtures;

/** FR-031/FR-032: the manual consultant transition table, reasons, readiness, and role gate. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultantManualTransitionTest {

    static final CurrentUser ADMIN = new CurrentUser(1L, "admin", "Alex Admin", Role.ADMIN, null);
    static final CurrentUser HR = new CurrentUser(6L, "hr", "Harper HR", Role.HR_OPERATIONS, null);
    static final CurrentUser MANAGER = new CurrentUser(2L, "manager", "Morgan", Role.MANAGER, null);
    static final CurrentUser RECRUITER = new CurrentUser(3L, "recruiter1", "Riya", Role.RECRUITER, 7L);

    @Mock
    ConsultantRepository consultants;
    @Mock
    HistoryService history;

    ConsultantLifecycleService lifecycle;

    @BeforeEach
    void setUp() {
        lifecycle = LifecycleTestSupport.lifecycle(consultants, history,
                Clock.fixed(ConsultantFixtures.NOW, ZoneId.of("America/New_York")));
    }

    private Consultant stub(ConsultantStatus status) {
        Consultant c = consultant(42, status, completeProfile("c@x.com"), recruiter(7, RecruiterStatus.ACTIVE));
        when(consultants.findByIdForUpdate(42L)).thenReturn(Optional.of(c));
        when(consultants.findById(42L)).thenReturn(Optional.of(c));
        return c;
    }

    static Stream<Arguments> allowed() {
        return Stream.of(
                Arguments.of(BENCH, READY, null),
                Arguments.of(READY, BENCH, null),
                Arguments.of(BENCH, HOLD, "Personal leave"),
                Arguments.of(READY, HOLD, "Personal leave"),
                Arguments.of(MARKETING, HOLD, "Personal leave"),
                Arguments.of(INTERVIEWING, HOLD, "Personal leave"),
                Arguments.of(HOLD, BENCH, null),
                Arguments.of(HOLD, READY, null),
                Arguments.of(BENCH, INACTIVE, "Left the company"),
                Arguments.of(READY, INACTIVE, "Left the company"),
                Arguments.of(HOLD, INACTIVE, "Left the company"),
                Arguments.of(ACTIVE_PROJECT, INACTIVE, "Left the company"),
                Arguments.of(INACTIVE, BENCH, null),
                Arguments.of(ACTIVE_PROJECT, BENCH, null));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allowed")
    void allowedTransitionsSucceedAndAreRecorded(ConsultantStatus from, ConsultantStatus to, String reason) {
        Consultant c = stub(from);
        Consultant result = lifecycle.changeStatusManually(42L, to, reason, 3L, HR);
        assertThat(result.getStatus()).isEqualTo(to);
        verify(history).recordStatusChange(eq(HistoryEntityType.CONSULTANT), eq(42L), eq(42L), eq(null),
                eq(from.name()), eq(to.name()), eq(reason), eq(null), eq(HR));
        assertThat(c.getStatus()).isEqualTo(to);
    }

    @Test
    void adminMayAlsoChangeStatusManually() {
        stub(BENCH);
        assertThat(lifecycle.changeStatusManually(42L, READY, null, 3L, ADMIN).getStatus()).isEqualTo(READY);
    }

    @ParameterizedTest
    @EnumSource(value = ConsultantStatus.class, names = { "MARKETING", "INTERVIEWING", "PLACED" })
    void automaticStatusesCanNeverBeManualTargets(ConsultantStatus target) {
        for (ConsultantStatus from : ConsultantStatus.values()) {
            if (from == target) {
                continue;
            }
            stub(from);
            assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, target, "x", 3L, ADMIN))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_TRANSITION));
        }
        verify(history, never()).recordStatusChange(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void disallowedTransitionReportsCurrentAndAllowed() {
        stub(INACTIVE);
        assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, READY, null, 3L, HR))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_TRANSITION);
                    assertThat(e.getExtras()).containsEntry("currentStatus", INACTIVE);
                    assertThat(e.getExtras().get("allowedTransitions")).isEqualTo(List.of(BENCH));
                });
    }

    @Test
    void readyToHoldWithoutReasonFailsValidation() {
        stub(READY);
        assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, HOLD, "  ", 3L, HR))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(e.getFieldErrors()).extracting("field").containsExactly("reason");
                });
    }

    @Test
    void inactiveWithoutReasonFailsValidation() {
        stub(BENCH);
        assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, INACTIVE, null, 3L, HR))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void readyRequiresCompleteProfile() {
        Consultant c = consultant(42, BENCH, minimalProfile("m@x.com"), null);
        when(consultants.findByIdForUpdate(42L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, READY, null, 3L, HR))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.READINESS_INCOMPLETE);
                    assertThat((List<Object>) e.getExtras().get("missingItems")).contains("phone",
                            "assignedActiveRecruiter");
                });
        assertThat(c.getStatus()).isEqualTo(BENCH);
    }

    @Test
    void holdToReadyAlsoRequiresReadiness() {
        Consultant c = consultant(42, HOLD, completeProfile("c@x.com"), recruiter(7, RecruiterStatus.INACTIVE));
        when(consultants.findByIdForUpdate(42L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, READY, null, 3L, HR))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.READINESS_INCOMPLETE));
    }

    @Test
    void staleVersionIsRefused() {
        stub(BENCH);
        assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, HOLD, "x", 2L, HR))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION));
    }

    @Test
    void managerAndRecruiterAreNotAuthorized() {
        stub(BENCH);
        assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, HOLD, "x", 3L, MANAGER))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> lifecycle.changeStatusManually(42L, HOLD, "x", 3L, RECRUITER))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(history);
    }

    @Test
    void allowedManualTransitionsDependOnRole() {
        Consultant c = consultant(42, BENCH, completeProfile("c@x.com"), recruiter(7, RecruiterStatus.ACTIVE));
        assertThat(lifecycle.allowedManualTransitions(c, HR)).containsExactly(READY, HOLD, INACTIVE);
        assertThat(lifecycle.allowedManualTransitions(c, MANAGER)).isEmpty();
        assertThat(lifecycle.allowedManualTransitions(c, RECRUITER)).isEmpty();
        Consultant placed = consultant(43, PLACED, completeProfile("p@x.com"), null);
        assertThat(lifecycle.allowedManualTransitions(placed, ADMIN)).doesNotContain(MARKETING, INTERVIEWING, PLACED);
    }
}
