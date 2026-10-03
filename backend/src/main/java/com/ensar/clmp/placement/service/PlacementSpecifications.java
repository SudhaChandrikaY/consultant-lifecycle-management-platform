package com.ensar.clmp.placement.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.common.time.OrgTime;
import com.ensar.clmp.placement.domain.Placement;

public final class PlacementSpecifications {

    private PlacementSpecifications() {
    }

    public static Specification<Placement> matching(PlacementFilter f, OrgTime orgTime) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.recruiterId() != null) {
                p.add(cb.equal(root.get("recruiter").get("id"), f.recruiterId()));
            }
            if (f.clientId() != null) {
                p.add(cb.equal(root.get("client").get("id"), f.clientId()));
            }
            if (f.vendorId() != null) {
                p.add(cb.equal(root.get("vendor").get("id"), f.vendorId()));
            }
            if (f.startFrom() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("startDate"), f.startFrom()));
            }
            if (f.startTo() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("startDate"), f.startTo()));
            }
            if (f.createdFrom() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), orgTime.startOf(f.createdFrom())));
            }
            if (f.createdTo() != null) {
                p.add(cb.lessThan(root.get("createdAt"), orgTime.endOf(f.createdTo())));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** FR-073: a RECRUITER sees only placements where they are the placement's recruiter. */
    public static Specification<Placement> scopeFor(CurrentUser user) {
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
            return cb.equal(root.get("recruiter").get("id"), user.recruiterId());
        };
    }
}
