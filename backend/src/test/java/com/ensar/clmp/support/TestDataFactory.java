package com.ensar.clmp.support;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.domain.AppUserRepository;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantProfile;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;
import com.ensar.clmp.reference.domain.RegionRepository;
import com.ensar.clmp.reference.domain.TeamRepository;
import com.ensar.clmp.reference.domain.VisaType;

/** Creates test records directly through repositories. Grows with each story. */
@Component
public class TestDataFactory {

    private static final AtomicInteger SEQ = new AtomicInteger();

    protected final Clock clock;
    protected final AppUserRepository users;
    protected final RecruiterRepository recruiters;
    protected final TeamRepository teams;
    protected final RegionRepository regions;
    protected final ConsultantRepository consultants;

    public TestDataFactory(Clock clock, AppUserRepository users, RecruiterRepository recruiters,
            TeamRepository teams, RegionRepository regions, ConsultantRepository consultants) {
        this.clock = clock;
        this.users = users;
        this.recruiters = recruiters;
        this.teams = teams;
        this.regions = regions;
        this.consultants = consultants;
    }

    @Transactional(readOnly = true)
    public Long recruiterIdOfUser(String username) {
        Long userId = users.findByUsernameIgnoreCase(username).orElseThrow().getId();
        return recruiters.findByUserId(userId).map(Recruiter::getId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Long recruiterIdByName(String fullName) {
        return recruiters.findAll().stream().filter(r -> r.getFullName().equals(fullName)).findFirst()
                .orElseThrow().getId();
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "." + SEQ.incrementAndGet() + "@test.example";
    }

    /** A complete profile (Ready-eligible once an active recruiter is assigned). */
    public static ConsultantProfile completeProfile(String first, String last, String skill) {
        return new ConsultantProfile(first, last, uniqueEmail(first.toLowerCase()), "555-0100", "Austin", "TX", skill,
                "SQL", 6, VisaType.GREEN_CARD, null, "Test notes");
    }

    /** Saves a consultant directly in the given status, optionally assigned to a recruiter. */
    @Transactional
    public Long consultant(ConsultantProfile profile, ConsultantStatus status, Long recruiterId) {
        Consultant c = new Consultant(profile, null, clock.instant());
        if (recruiterId != null) {
            c.assignRecruiter(recruiters.findById(recruiterId).orElseThrow(), clock.instant());
        }
        c.changeStatus(status, clock.instant());
        return consultants.save(c).getId();
    }

    @Transactional
    public void assignRecruiter(Long consultantId, Long recruiterId) {
        Consultant c = consultants.findById(consultantId).orElseThrow();
        c.assignRecruiter(recruiters.findById(recruiterId).orElseThrow(), clock.instant());
    }

    @Transactional(readOnly = true)
    public ConsultantStatus consultantStatus(Long consultantId) {
        return consultants.findById(consultantId).orElseThrow().getStatus();
    }

    @Transactional(readOnly = true)
    public long consultantVersion(Long consultantId) {
        return consultants.findById(consultantId).orElseThrow().getVersion();
    }
}
