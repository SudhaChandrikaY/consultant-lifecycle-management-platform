package com.ensar.clmp.submission.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.common.web.PersonRef;
import com.ensar.clmp.submission.domain.Submission;
import com.ensar.clmp.submission.domain.SubmissionStatus;

public record SubmissionListItem(Long id, PersonRef consultant, PersonRef recruiter, NamedRef vendor, NamedRef client,
        String jobTitle, LocalDate submittedDate, BigDecimal billRate, SubmissionStatus status) {

    public static SubmissionListItem from(Submission s) {
        return new SubmissionListItem(s.getId(), new PersonRef(s.getConsultant().getId(), s.getConsultant().getFullName()),
                new PersonRef(s.getRecruiter().getId(), s.getRecruiter().getFullName()),
                new NamedRef(s.getVendor().getId(), s.getVendor().getName()),
                new NamedRef(s.getClient().getId(), s.getClient().getName()), s.getJobTitle(), s.getSubmittedDate(),
                s.getBillRate(), s.getStatus());
    }
}
