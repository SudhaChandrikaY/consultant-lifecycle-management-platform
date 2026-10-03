package com.ensar.clmp.consultant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.consultant.domain.Consultant;
import com.ensar.clmp.recruiter.domain.Recruiter;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;

public final class ConsultantSpecifications {

    private ConsultantSpecifications() {
    }

    public static Specification<Consultant> matching(ConsultantFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))),
                                like)));
            }
            if (f.statuses() != null && !f.statuses().isEmpty()) {
                predicates.add(root.get("status").in(f.statuses()));
            }
            if (f.primarySkill() != null && !f.primarySkill().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("primarySkill")),
                        f.primarySkill().trim().toLowerCase(Locale.ROOT)));
            }
            if (f.visaType() != null) {
                predicates.add(cb.equal(root.get("visaType"), f.visaType()));
            }
            if (f.recruiterId() != null) {
                predicates.add(cb.equal(root.get("currentRecruiter").get("id"), f.recruiterId()));
            }
            if (f.needsReassignment() != null) {
                Join<Consultant, Recruiter> recruiter = root.join("currentRecruiter", JoinType.LEFT);
                Predicate flagged = cb.equal(recruiter.get("status"), RecruiterStatus.INACTIVE);
                predicates.add(f.needsReassignment() ? flagged
                        : cb.or(cb.isNull(recruiter.get("id")), cb.notEqual(recruiter.get("status"),
                                RecruiterStatus.INACTIVE)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * ADMIN, MANAGER, and HR see every consultant. A RECRUITER sees consultants currently assigned
     * to their linked recruiter profile; an unlinked RECRUITER sees nothing.
     */
    public static Specification<Consultant> scopeFor(CurrentUser user) {
        return (root, query, cb) -> {
            if (user.role() != Role.RECRUITER) {
                return cb.conjunction();
            }
            if (user.recruiterId() == null) {
                return cb.disjunction();
            }
            return cb.equal(root.get("currentRecruiter").get("id"), user.recruiterId());
        };
    }
}
