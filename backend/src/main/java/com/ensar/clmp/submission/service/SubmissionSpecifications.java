package com.ensar.clmp.submission.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.submission.domain.Submission;

public final class SubmissionSpecifications {

    private SubmissionSpecifications() {
    }

    public static Specification<Submission> matching(SubmissionFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.statuses() != null && !f.statuses().isEmpty()) {
                p.add(root.get("status").in(f.statuses()));
            }
            if (f.recruiterId() != null) {
                p.add(cb.equal(root.get("recruiter").get("id"), f.recruiterId()));
            }
            if (f.vendorId() != null) {
                p.add(cb.equal(root.get("vendor").get("id"), f.vendorId()));
            }
            if (f.clientId() != null) {
                p.add(cb.equal(root.get("client").get("id"), f.clientId()));
            }
            if (f.consultantId() != null) {
                p.add(cb.equal(root.get("consultant").get("id"), f.consultantId()));
            }
            if (f.submittedFrom() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("submittedDate"), f.submittedFrom()));
            }
            if (f.submittedTo() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("submittedDate"), f.submittedTo()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** FR-063: a RECRUITER sees submissions they made or for consultants now assigned to them. */
    public static Specification<Submission> scopeFor(CurrentUser user) {
        return (root, query, cb) -> {
            if (user.role() == Role.HR_OPERATIONS) {
                return cb.disjunction();
            }
            if (user.role() != Role.RECRUITER) {
                return cb.conjunction();
            }
            if (user.recruiterId() == null) {
                return cb.disjunction();
            }
            return cb.or(cb.equal(root.get("recruiter").get("id"), user.recruiterId()),
                    cb.equal(root.get("consultant").get("currentRecruiter").get("id"), user.recruiterId()));
        };
    }
}
