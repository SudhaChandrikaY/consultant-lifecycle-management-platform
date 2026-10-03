package com.ensar.clmp.support;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.domain.AppUserRepository;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;
import com.ensar.clmp.reference.domain.RegionRepository;
import com.ensar.clmp.reference.domain.TeamRepository;

/** Creates test records directly through repositories. Grows with each story. */
@Component
public class TestDataFactory {

    protected final Clock clock;
    protected final AppUserRepository users;
    protected final RecruiterRepository recruiters;
    protected final TeamRepository teams;
    protected final RegionRepository regions;

    public TestDataFactory(Clock clock, AppUserRepository users, RecruiterRepository recruiters,
            TeamRepository teams, RegionRepository regions) {
        this.clock = clock;
        this.users = users;
        this.recruiters = recruiters;
        this.teams = teams;
        this.regions = regions;
    }

    @Transactional(readOnly = true)
    public Long recruiterIdOfUser(String username) {
        Long userId = users.findByUsernameIgnoreCase(username).orElseThrow().getId();
        return recruiters.findByUserId(userId).map(Recruiter::getId).orElseThrow();
    }
}
