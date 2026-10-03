package com.ensar.clmp.seed;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.domain.AppUser;
import com.ensar.clmp.auth.domain.AppUserRepository;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;
import com.ensar.clmp.reference.domain.Region;
import com.ensar.clmp.reference.domain.RegionRepository;
import com.ensar.clmp.reference.domain.Team;
import com.ensar.clmp.reference.domain.TeamRepository;

/**
 * Seeds demo reference data, users, and workflow records for the {@code dev} profile (research
 * R17). Demo passwords are for local development only; the seeder refuses to run under
 * {@code prod}. It skips seeding when users already exist (file profile restarts).
 */
@Component
@Profile("dev")
@Order(1)
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Demo@123";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final Environment environment;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final TeamRepository teams;
    private final RegionRepository regions;
    private final AppUserRepository users;
    private final RecruiterRepository recruiters;

    public DemoDataSeeder(Environment environment, PasswordEncoder passwordEncoder, Clock clock,
            TeamRepository teams, RegionRepository regions, AppUserRepository users,
            RecruiterRepository recruiters) {
        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.teams = teams;
        this.regions = regions;
        this.users = users;
        this.recruiters = recruiters;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("DemoDataSeeder must not run with the prod profile active.");
        }
        if (users.count() > 0) {
            log.info("Demo data already present; skipping seed.");
            return;
        }
        seedReference();
        seedUsersAndRecruiters();
        log.info("Demo data seeded.");
    }

    private void seedReference() {
        teams.save(new Team("JAVA", "Java"));
        teams.save(new Team("DOTNET", ".NET"));
        teams.save(new Team("DATA", "Data & Analytics"));
        teams.save(new Team("DEVOPS", "DevOps & Cloud"));
        regions.save(new Region("EAST", "East"));
        regions.save(new Region("CENTRAL", "Central"));
        regions.save(new Region("WEST", "West"));
        regions.save(new Region("OFFSHORE", "Offshore"));
    }

    private void seedUsersAndRecruiters() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        users.save(new AppUser("admin", hash, "Alex Admin", Role.ADMIN, true));
        users.save(new AppUser("manager", hash, "Morgan Manager", Role.MANAGER, true));
        AppUser recruiter1 = users.save(new AppUser("recruiter1", hash, "Riya Patel", Role.RECRUITER, true));
        AppUser recruiter2 = users.save(new AppUser("recruiter2", hash, "Marcus Lee", Role.RECRUITER, true));
        users.save(new AppUser("recruiter3", hash, "Sam Unlinked", Role.RECRUITER, true));
        users.save(new AppUser("hr", hash, "Harper HR", Role.HR_OPERATIONS, true));
        users.save(new AppUser("inactive.user", hash, "Ina Active", Role.RECRUITER, false));

        Instant now = clock.instant();
        recruiters.save(new Recruiter("Riya Patel", "riya.patel@clmp.example", "555-0101", team("JAVA"),
                region("EAST"), recruiter1, now));
        recruiters.save(new Recruiter("Marcus Lee", "marcus.lee@clmp.example", "555-0102", team("DATA"),
                region("CENTRAL"), recruiter2, now));
        recruiters.save(new Recruiter("Priya Shah", "priya.shah@clmp.example", "555-0103", team("DOTNET"),
                region("WEST"), null, now));
        recruiters.save(new Recruiter("Daniel Kim", "daniel.kim@clmp.example", "555-0104", team("DEVOPS"),
                region("OFFSHORE"), null, now));
    }

    private Team team(String code) {
        return teams.findByCode(code).orElseThrow();
    }

    private Region region(String code) {
        return regions.findByCode(code).orElseThrow();
    }
}
