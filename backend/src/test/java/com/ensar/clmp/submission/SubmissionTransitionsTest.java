package com.ensar.clmp.submission;

import static com.ensar.clmp.submission.domain.SubmissionStatus.DRAFT;
import static com.ensar.clmp.submission.domain.SubmissionStatus.INTERVIEW_CLEARED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.INTERVIEW_SCHEDULED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.OFFER;
import static com.ensar.clmp.submission.domain.SubmissionStatus.PLACED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.REJECTED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.SUBMITTED;
import static com.ensar.clmp.submission.domain.SubmissionStatus.UNDER_REVIEW;
import static com.ensar.clmp.submission.domain.SubmissionStatus.WITHDRAWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.submission.domain.SubmissionStatus;
import com.ensar.clmp.submission.service.SubmissionTransitions;

/** FR-056–FR-058 submission state machine and status groupings. */
class SubmissionTransitionsTest {

    private final SubmissionTransitions transitions = new SubmissionTransitions();

    private static final Map<SubmissionStatus, List<SubmissionStatus>> EXPECTED = Map.of(
            DRAFT, List.of(SUBMITTED, WITHDRAWN),
            SUBMITTED, List.of(UNDER_REVIEW, INTERVIEW_SCHEDULED, REJECTED, WITHDRAWN),
            UNDER_REVIEW, List.of(INTERVIEW_SCHEDULED, REJECTED, WITHDRAWN),
            INTERVIEW_SCHEDULED, List.of(INTERVIEW_CLEARED, REJECTED, WITHDRAWN),
            INTERVIEW_CLEARED, List.of(INTERVIEW_SCHEDULED, OFFER, REJECTED, WITHDRAWN),
            OFFER, List.of(REJECTED, WITHDRAWN),
            REJECTED, List.of(),
            WITHDRAWN, List.of(),
            PLACED, List.of());

    @Test
    void everyFr058EdgeIsAllowedAndNothingElse() {
        for (SubmissionStatus from : SubmissionStatus.values()) {
            assertThat(transitions.allowedFrom(from)).as("from %s", from).containsExactlyElementsOf(EXPECTED.get(from));
            for (SubmissionStatus to : SubmissionStatus.values()) {
                if (EXPECTED.get(from).contains(to)) {
                    assertThatCode(() -> transitions.validate(from, to)).doesNotThrowAnyException();
                } else {
                    assertThatThrownBy(() -> transitions.validate(from, to))
                            .as("%s -> %s", from, to)
                            .isInstanceOfSatisfying(BusinessException.class, e -> {
                                assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_TRANSITION);
                                assertThat(e.getExtras()).containsEntry("currentStatus", from)
                                        .containsEntry("allowedTransitions", EXPECTED.get(from));
                            });
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(SubmissionStatus.class)
    void manualPlacedIsAlwaysRefused(SubmissionStatus from) {
        assertThatThrownBy(() -> transitions.validate(from, PLACED)).isInstanceOf(BusinessException.class);
    }

    @Test
    void terminalStatuses() {
        assertThat(SubmissionStatus.TERMINAL).containsExactlyInAnyOrder(REJECTED, WITHDRAWN, PLACED);
        for (SubmissionStatus s : SubmissionStatus.TERMINAL) {
            assertThat(transitions.allowedFrom(s)).isEmpty();
        }
    }

    @Test
    void groupings() {
        assertThat(SubmissionStatus.ACTIVE)
                .containsExactlyInAnyOrder(SUBMITTED, UNDER_REVIEW, INTERVIEW_SCHEDULED, INTERVIEW_CLEARED, OFFER);
        assertThat(SubmissionStatus.INTERVIEW_STAGE).containsExactlyInAnyOrder(INTERVIEW_SCHEDULED, INTERVIEW_CLEARED);
        assertThat(SubmissionStatus.INTERVIEW_OR_OFFER)
                .containsExactlyInAnyOrder(INTERVIEW_SCHEDULED, INTERVIEW_CLEARED, OFFER);
        assertThat(SubmissionStatus.OPEN)
                .isEqualTo(EnumSet.complementOf(EnumSet.of(REJECTED, WITHDRAWN, PLACED)));
    }
}
