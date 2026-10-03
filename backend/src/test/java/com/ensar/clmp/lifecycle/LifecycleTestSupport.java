package com.ensar.clmp.lifecycle;

import java.time.Clock;

import com.ensar.clmp.common.service.VersionGuard;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.service.ReadinessChecker;
import com.ensar.clmp.history.service.HistoryService;

/** Builds a ConsultantLifecycleService for unit tests; collaborators added by later stories are mocked here. */
final class LifecycleTestSupport {

    private LifecycleTestSupport() {
    }

    static ConsultantLifecycleService lifecycle(ConsultantRepository consultants, HistoryService history, Clock clock) {
        return new ConsultantLifecycleService(consultants, history, new ReadinessChecker(), new VersionGuard(), clock);
    }
}
