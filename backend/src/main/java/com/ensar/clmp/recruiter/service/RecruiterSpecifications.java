package com.ensar.clmp.recruiter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ensar.clmp.recruiter.domain.Recruiter;

public final class RecruiterSpecifications {

    private RecruiterSpecifications() {
    }

    public static Specification<Recruiter> matching(RecruiterFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ") + "%";
                predicates.add(cb.like(cb.lower(root.get("fullName")), like));
            }
            if (f.teamId() != null) {
                predicates.add(cb.equal(root.get("team").get("id"), f.teamId()));
            }
            if (f.regionId() != null) {
                predicates.add(cb.equal(root.get("region").get("id"), f.regionId()));
            }
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
