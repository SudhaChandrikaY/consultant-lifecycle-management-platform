package com.ensar.clmp.consultant;

import static com.ensar.clmp.support.ConsultantFixtures.completeProfile;
import static com.ensar.clmp.support.ConsultantFixtures.consultant;
import static com.ensar.clmp.support.ConsultantFixtures.minimalProfile;
import static com.ensar.clmp.support.ConsultantFixtures.recruiter;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.consultant.service.ReadinessChecker;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;

/** FR-035: the ordered list of items still missing before a consultant can be Ready. */
class ReadinessCheckerTest {

    private final ReadinessChecker checker = new ReadinessChecker();

    @Test
    void minimalProfileWithoutRecruiterListsEveryMissingItemInOrder() {
        var c = consultant(1, ConsultantStatus.BENCH, minimalProfile("a@x.com"), null);
        assertThat(checker.missingItems(c)).containsExactly("phone", "primarySkill", "yearsExperience", "visaType",
                "assignedActiveRecruiter");
    }

    @Test
    void completeProfileWithActiveRecruiterHasNothingMissing() {
        var c = consultant(1, ConsultantStatus.BENCH, completeProfile("a@x.com"), recruiter(7, RecruiterStatus.ACTIVE));
        assertThat(checker.missingItems(c)).isEmpty();
    }

    @Test
    void inactiveAssignedRecruiterCountsAsMissing() {
        var c = consultant(1, ConsultantStatus.BENCH, completeProfile("a@x.com"),
                recruiter(7, RecruiterStatus.INACTIVE));
        assertThat(checker.missingItems(c)).containsExactly("assignedActiveRecruiter");
    }

    @Test
    void blankStringsCountAsMissing() {
        var profile = new com.ensar.clmp.consultant.domain.ConsultantProfile("  ", "Last", "e@x.com", " ", null, null,
                "   ", null, 0, com.ensar.clmp.reference.domain.VisaType.OPT, null, null);
        var c = consultant(1, ConsultantStatus.BENCH, profile, recruiter(7, RecruiterStatus.ACTIVE));
        assertThat(checker.missingItems(c)).containsExactly("firstName", "phone", "primarySkill");
    }
}
