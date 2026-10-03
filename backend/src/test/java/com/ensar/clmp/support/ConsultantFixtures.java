package com.ensar.clmp.support;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.consultant.domain.ConsultantProfile;
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;
import com.ensar.clmp.reference.domain.Region;
import com.ensar.clmp.reference.domain.Team;
import com.ensar.clmp.reference.domain.VisaType;

/** In-memory (unsaved) entities for unit tests. */
public final class ConsultantFixtures {

    public static final Instant NOW = Instant.parse("2026-10-15T15:00:00Z");

    private ConsultantFixtures() {
    }

    public static ConsultantProfile completeProfile(String email) {
        return new ConsultantProfile("Arun", "Kumar", email, "555-0199", "Edison", "NJ", "Java", "Spring, Kafka", 8,
                VisaType.H1B, LocalDate.of(2027, 5, 31), "Strong backend engineer");
    }

    public static ConsultantProfile minimalProfile(String email) {
        return new ConsultantProfile("Min", "Imal", email, null, null, null, null, null, null, null, null, null);
    }

    public static Recruiter recruiter(long id, RecruiterStatus status) {
        Recruiter r = new Recruiter("Recruiter " + id, "r" + id + "@clmp.example", null, new Team("T" + id, "Team"),
                new Region("R" + id, "Region"), null, NOW);
        ReflectionTestUtils.setField(r, "id", id);
        r.changeStatus(status, NOW);
        return r;
    }

    public static Consultant consultant(long id, ConsultantStatus status, ConsultantProfile profile,
            Recruiter recruiter) {
        Consultant c = new Consultant(profile, 1L, NOW);
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "version", 3L);
        ReflectionTestUtils.setField(c, "status", status);
        if (recruiter != null) {
            c.assignRecruiter(recruiter, NOW);
        }
        return c;
    }
}
