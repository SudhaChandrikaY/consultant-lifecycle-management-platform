package com.ensar.clmp.marketing.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.marketing.domain.MarketingAssignment;
import com.ensar.clmp.marketing.domain.MarketingStatus;

public final class MarketingSpecifications {

    private MarketingSpecifications() {
    }

    public static Specification<MarketingAssignment> matching(MarketingFilter f, LocalDate today) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.statuses() != null && !f.statuses().isEmpty()) {
                predicates.add(root.get("status").in(f.statuses()));
            }
            if (f.recruiterId() != null) {
                predicates.add(cb.equal(root.get("ownerRecruiter").get("id"), f.recruiterId()));
            }
            if (f.teamId() != null) {
                predicates.add(cb.equal(root.get("ownerTeam").get("id"), f.teamId()));
            }
            if (f.overdue() != null) {
                Predicate overdue = cb.and(cb.notEqual(root.get("status"), MarketingStatus.CLOSED),
                        cb.lessThan(root.get("targetDate"), today));
                predicates.add(f.overdue() ? overdue : cb.not(overdue));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** RECRUITER: assignments whose consultant is currently assigned to them. HR: nothing. */
    public static Specification<MarketingAssignment> scopeFor(CurrentUser user) {
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
            return cb.equal(root.get("consultant").get("currentRecruiter").get("id"), user.recruiterId());
        };
    }
}
