package com.ensar.clmp.lifecycle;

import static org.mockito.Mockito.mock;

import java.time.Clock;

import com.ensar.clmp.common.service.VersionGuard;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.service.ReadinessChecker;
import com.ensar.clmp.history.service.HistoryService;
import com.ensar.clmp.marketing.domain.MarketingAssignmentRepository;

/** Builds a ConsultantLifecycleService for unit tests; collaborators added by later stories are mocked here. */
final class LifecycleTestSupport {

    private LifecycleTestSupport() {
    }

    static ConsultantLifecycleService lifecycle(ConsultantRepository consultants, HistoryService history, Clock clock) {
        return new ConsultantLifecycleService(consultants, mock(MarketingAssignmentRepository.class), history,
                new ReadinessChecker(), new VersionGuard(), clock);
    }
}
