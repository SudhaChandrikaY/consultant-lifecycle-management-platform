package com.ensar.clmp.seed;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

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

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.AppUser;
import com.ensar.clmp.auth.domain.AppUserRepository;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.consultant.service.ConsultantAssignmentService;
import com.ensar.clmp.consultant.service.ConsultantService;
import com.ensar.clmp.consultant.web.ConsultantRequest;
import com.ensar.clmp.lifecycle.ConsultantLifecycleService;
import com.ensar.clmp.marketing.domain.MarketingAssignmentRepository;
import com.ensar.clmp.marketing.domain.MarketingStatus;
import com.ensar.clmp.marketing.service.MarketingService;
import com.ensar.clmp.marketing.web.MarketingCreateRequest;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;
import com.ensar.clmp.reference.domain.Region;
import com.ensar.clmp.reference.domain.RegionRepository;
import com.ensar.clmp.reference.domain.Team;
import com.ensar.clmp.reference.domain.TeamRepository;
import com.ensar.clmp.reference.domain.VisaType;

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
    private final ConsultantRepository consultants;
    private final ConsultantService consultantService;
    private final ConsultantLifecycleService lifecycle;
    private final ConsultantAssignmentService assignment;
    private final MarketingService marketingService;
    private final MarketingAssignmentRepository marketing;

    public DemoDataSeeder(Environment environment, PasswordEncoder passwordEncoder, Clock clock,
            TeamRepository teams, RegionRepository regions, AppUserRepository users,
            RecruiterRepository recruiters, ConsultantRepository consultants, ConsultantService consultantService,
            ConsultantLifecycleService lifecycle, ConsultantAssignmentService assignment,
            MarketingService marketingService, MarketingAssignmentRepository marketing) {
        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.teams = teams;
        this.regions = regions;
        this.users = users;
        this.recruiters = recruiters;
        this.consultants = consultants;
        this.consultantService = consultantService;
        this.lifecycle = lifecycle;
        this.assignment = assignment;
        this.marketingService = marketingService;
        this.marketing = marketing;
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
        seedConsultants();
        seedMarketing();
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

    /**
     * About a dozen consultants across Bench, Ready, Hold, and Inactive with complete and
     * incomplete profiles. Arun Kumar is a complete Bench consultant assigned to Riya Patel (AS 2.3
     * demo); Ana Souza is assigned to an unlinked recruiter.
     */
    private void seedConsultants() {
        Long riya = recruiterId("Riya Patel");
        Long marcus = recruiterId("Marcus Lee");
        Long priya = recruiterId("Priya Shah");
        Long daniel = recruiterId("Daniel Kim");

        consultant("Arun", "Kumar", "Java", 8, VisaType.H1B, riya, ConsultantStatus.BENCH, null);
        consultant("Meera", "Iyer", "Java", 6, VisaType.GREEN_CARD, riya, ConsultantStatus.READY, null);
        consultant("Vikram", "Rao", "Spring Boot", 10, VisaType.H1B, riya, ConsultantStatus.READY, null);
        consultant("Sofia", "Martinez", "Java", 5, VisaType.US_CITIZEN, riya, ConsultantStatus.READY, null);
        consultant("Kevin", "Obrien", "Microservices", 7, VisaType.US_CITIZEN, riya, ConsultantStatus.HOLD,
                "Personal leave until next month");
        consultant("Li", "Wei", "Python", 4, VisaType.STEM_OPT, marcus, ConsultantStatus.READY, null);
        consultant("Fatima", "Noor", "Data Engineering", 9, VisaType.H4_EAD, marcus, ConsultantStatus.BENCH, null);
        consultant("Jonas", "Berg", "Spark", 12, VisaType.GREEN_CARD, marcus, ConsultantStatus.INACTIVE,
                "Accepted a full-time role elsewhere");
        consultant("Ana", "Souza", ".NET", 6, VisaType.L2_EAD, priya, ConsultantStatus.READY, null);
        consultant("Tom", "Becker", "Kubernetes", 3, VisaType.TN, daniel, ConsultantStatus.BENCH, null);

        // Incomplete profiles (missing phone, skill, experience, visa, and recruiter).
        create(new ConsultantRequest("Grace", "Lin", "grace.lin@consultants.example", null, null, null, null,
                null, null, null, null, null, null));
        create(new ConsultantRequest("Omar", "Haddad", "omar.haddad@consultants.example", "555-0312", "Dallas",
                "TX", "React", null, 5, null, null, null, null));
    }

    private Long consultant(String first, String last, String skill, int years, VisaType visa, Long recruiterId,
            ConsultantStatus target, String reason) {
        String email = (first + "." + last).toLowerCase() + "@consultants.example";
        Long id = create(new ConsultantRequest(first, last, email, "555-02" + String.format("%02d", consultants.count()),
                "Edison", "NJ", skill, "SQL, Git", years, visa, null, "Seeded demo consultant", null));
        assign(id, recruiterId);
        if (target == ConsultantStatus.HOLD || target == ConsultantStatus.INACTIVE) {
            setStatus(id, ConsultantStatus.READY, null);
        }
        if (target != ConsultantStatus.BENCH) {
            setStatus(id, target, reason);
        }
        return id;
    }

    /**
     * Active (one overdue), Hold, and Closed marketing assignments. Meera Iyer stays Ready with no
     * assignment so the US4 walkthrough can start one.
     */
    private void seedMarketing() {
        LocalDate today = LocalDate.now(clock);
        Long vikram = consultantId("Vikram");
        Long sofia = consultantId("Sofia");
        Long li = consultantId("Li");
        Long ana = consultantId("Ana");

        Long overdue = startMarketing(vikram, "recruiter1", today.minusDays(30), today.minusDays(3));
        transitionMarketing(overdue, MarketingStatus.ACTIVE, null, "recruiter1");

        Long held = startMarketing(sofia, "recruiter1", today.minusDays(10), today.plusDays(20));
        transitionMarketing(held, MarketingStatus.ACTIVE, null, "recruiter1");
        transitionMarketing(held, MarketingStatus.HOLD, "Client hiring freeze until next quarter", "recruiter1");

        Long active = startMarketing(li, "recruiter2", today.minusDays(7), today.plusDays(30));
        transitionMarketing(active, MarketingStatus.ACTIVE, null, "recruiter2");
        marketingService.addNote(active, "Shared profile with three Data vendors.", actor("recruiter2"));

        Long closed = startMarketing(ana, "admin", today.minusDays(40), today.minusDays(10));
        transitionMarketing(closed, MarketingStatus.ACTIVE, null, "admin");
        transitionMarketing(closed, MarketingStatus.CLOSED, "Paused at the consultant's request", "admin");
    }

    private Long startMarketing(Long consultantId, String username, LocalDate start, LocalDate target) {
        return marketingService.create(new MarketingCreateRequest(consultantId, null, start, target), actor(username))
                .id();
    }

    private void transitionMarketing(Long id, MarketingStatus target, String reason, String username) {
        long version = marketing.findById(id).orElseThrow().getVersion();
        marketingService.transition(id, target, reason, version, actor(username));
    }

    private Long consultantId(String firstName) {
        return consultants.findAll().stream().filter(c -> c.getFirstName().equals(firstName)).findFirst()
                .orElseThrow().getId();
    }

    private Long create(ConsultantRequest request) {
        return consultantService.create(request, actor("hr")).id();
    }

    private void assign(Long consultantId, Long recruiterId) {
        long version = consultants.findById(consultantId).orElseThrow().getVersion();
        assignment.assign(consultantId, recruiterId, version, actor("admin"));
        consultants.flush();
    }

    private void setStatus(Long consultantId, ConsultantStatus target, String reason) {
        long version = consultants.findById(consultantId).orElseThrow().getVersion();
        lifecycle.changeStatusManually(consultantId, target, reason, version, actor("hr"));
        consultants.flush();
    }

    private CurrentUser actor(String username) {
        AppUser user = users.findByUsernameIgnoreCase(username).orElseThrow();
        Long recruiterId = recruiters.findByUserId(user.getId()).map(Recruiter::getId).orElse(null);
        return new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), recruiterId);
    }

    private Long recruiterId(String fullName) {
        return recruiters.findAll().stream().filter(r -> r.getFullName().equals(fullName)).findFirst()
                .orElseThrow().getId();
    }

    private Team team(String code) {
        return teams.findByCode(code).orElseThrow();
    }

    private Region region(String code) {
        return regions.findByCode(code).orElseThrow();
    }
}
