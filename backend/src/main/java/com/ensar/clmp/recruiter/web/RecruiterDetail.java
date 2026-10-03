package com.ensar.clmp.recruiter.web;

import com.ensar.clmp.recruiter.domain.RecruiterStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/** List item fields plus contact, linked user, and version. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecruiterDetail(Long id, String fullName, RecruiterListItem.Ref team, RecruiterListItem.Ref region,
        RecruiterStatus status, long assignedConsultantCount, String email, String phone, LinkableUser linkedUser,
        Long version, Long consultantsFlaggedForReassignment) {
}
