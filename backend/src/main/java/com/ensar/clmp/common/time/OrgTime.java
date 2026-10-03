package com.ensar.clmp.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

/** Business dates in the organization time zone (research R15). */
@Service
public class OrgTime {

    private final Clock clock;

    public OrgTime(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDate currentMonthStart() {
        return YearMonth.from(today()).atDay(1);
    }

    public LocalDate currentMonthEnd() {
        return YearMonth.from(today()).atEndOfMonth();
    }

    public LocalDate toOrgDate(Instant instant) {
        return instant.atZone(clock.getZone()).toLocalDate();
    }

    /** First instant of {@code date} in the org zone. */
    public Instant startOf(LocalDate date) {
        return date.atStartOfDay(clock.getZone()).toInstant();
    }

    /** First instant after {@code date} ends in the org zone (exclusive upper bound). */
    public Instant endOf(LocalDate date) {
        return date.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
    }
}
