package com.ensar.clmp.seed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantProfile;
import com.ensar.clmp.consultant.domain.ConsultantRepository;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterRepository;
import com.ensar.clmp.reference.domain.Client;
import com.ensar.clmp.reference.domain.ClientRepository;
import com.ensar.clmp.reference.domain.Region;
import com.ensar.clmp.reference.domain.RegionRepository;
import com.ensar.clmp.reference.domain.Team;
import com.ensar.clmp.reference.domain.TeamRepository;
import com.ensar.clmp.reference.domain.Vendor;
import com.ensar.clmp.reference.domain.VendorRepository;
import com.ensar.clmp.reference.domain.VisaType;
import com.ensar.clmp.submission.domain.Submission;
import com.ensar.clmp.submission.domain.SubmissionRepository;
import com.ensar.clmp.submission.domain.SubmissionStatus;

/**
 * SC-007 volume seed for the {@code perf} profile (use with {@code dev}): tops the demo data up to
 * 50 recruiters, 500 consultants, and 2,000 submissions across statuses. It writes rows directly
 * (no workflow history) because it only exists to measure list, dashboard, and report latency.
 */
@Component
@Profile("perf")
@Order(2)
public class PerfDataSeeder implements ApplicationRunner {

    static final int RECRUITERS = 50;
    static final int CONSULTANTS = 500;
    static final int SUBMISSIONS = 2000;

    private static final Logger log = LoggerFactory.getLogger(PerfDataSeeder.class);
    private static final ConsultantStatus[] CONSULTANT_MIX = { ConsultantStatus.BENCH, ConsultantStatus.READY,
            ConsultantStatus.READY, ConsultantStatus.MARKETING, ConsultantStatus.MARKETING,
            ConsultantStatus.INTERVIEWING, ConsultantStatus.HOLD, ConsultantStatus.ACTIVE_PROJECT };
    private static final SubmissionStatus[] SUBMISSION_MIX = SubmissionStatus.values();

    private final Clock clock;
    private final TeamRepository teams;
    private final RegionRepository regions;
    private final RecruiterRepository recruiters;
    private final ConsultantRepository consultants;
    private final VendorRepository vendors;
    private final ClientRepository clients;
    private final SubmissionRepository submissions;

    public PerfDataSeeder(Clock clock, TeamRepository teams, RegionRepository regions, RecruiterRepository recruiters,
            ConsultantRepository consultants, VendorRepository vendors, ClientRepository clients,
            SubmissionRepository submissions) {
        this.clock = clock;
        this.teams = teams;
        this.regions = regions;
        this.recruiters = recruiters;
        this.consultants = consultants;
        this.vendors = vendors;
        this.clients = clients;
        this.submissions = submissions;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (consultants.count() >= CONSULTANTS) {
            log.info("Perf data already present; skipping.");
            return;
        }
        Random random = new Random(42);
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        List<Team> teamList = teams.findAll();
        List<Region> regionList = regions.findAll();

        List<Recruiter> recruiterList = new ArrayList<>(recruiters.findAll());
        for (int i = recruiterList.size(); i < RECRUITERS; i++) {
            recruiterList.add(recruiters.save(new Recruiter("Perf Recruiter " + i, "perf.recruiter." + i + "@clmp.example",
                    null, teamList.get(i % teamList.size()), regionList.get(i % regionList.size()), null, now)));
        }

        List<Vendor> vendorList = new ArrayList<>();
        List<Client> clientList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            vendorList.add(vendors.save(new Vendor("Perf Vendor " + i, null, now)));
            clientList.add(clients.save(new Client("Perf Client " + i, null, now)));
        }

        List<Consultant> consultantList = new ArrayList<>();
        for (int i = (int) consultants.count(); i < CONSULTANTS; i++) {
            Consultant c = new Consultant(new ConsultantProfile("Perf", "Consultant " + i,
                    "perf.consultant." + i + "@consultants.example", "555-9" + String.format("%03d", i % 1000), "Austin",
                    "TX", "Skill " + (i % 15), null, i % 20, VisaType.values()[i % VisaType.values().length], null,
                    null), null, now);
            c.assignRecruiter(recruiterList.get(i % recruiterList.size()), now);
            c.changeStatus(CONSULTANT_MIX[i % CONSULTANT_MIX.length], now);
            consultantList.add(consultants.save(c));
        }

        long existing = submissions.count();
        for (long i = existing; i < SUBMISSIONS; i++) {
            Consultant c = consultantList.get((int) (i % consultantList.size()));
            SubmissionStatus status = SUBMISSION_MIX[(int) (i % SUBMISSION_MIX.length)];
            if (status == SubmissionStatus.PLACED) {
                status = SubmissionStatus.OFFER;
            }
            LocalDate submitted = status == SubmissionStatus.DRAFT ? null : today.minusDays(random.nextInt(180));
            submissions.save(new Submission(c, c.getCurrentRecruiter(), vendorList.get((int) (i % 20)),
                    clientList.get((int) ((i * 7) % 20)), "Perf Role " + (i % 40),
                    BigDecimal.valueOf(60 + random.nextInt(60)), status, submitted, null, now));
        }
        log.info("Perf data seeded: {} recruiters, {} consultants, {} submissions.", recruiters.count(),
                consultants.count(), submissions.count());
    }
}
