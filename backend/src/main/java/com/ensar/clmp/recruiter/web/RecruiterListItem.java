package com.ensar.clmp.recruiter.web;

import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;

public record RecruiterListItem(Long id, String fullName, Ref team, Ref region, RecruiterStatus status,
        long assignedConsultantCount) {

    public record Ref(Long id, String name) {
    }

    public static RecruiterListItem from(Recruiter r, long assignedConsultantCount) {
        return new RecruiterListItem(r.getId(), r.getFullName(), new Ref(r.getTeam().getId(), r.getTeam().getName()),
                new Ref(r.getRegion().getId(), r.getRegion().getName()), r.getStatus(), assignedConsultantCount);
    }
}
