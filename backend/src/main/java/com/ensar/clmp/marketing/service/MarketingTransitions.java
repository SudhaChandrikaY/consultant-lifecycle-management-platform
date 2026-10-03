package com.ensar.clmp.marketing.service;

import static com.ensar.clmp.marketing.domain.MarketingStatus.ACTIVE;
import static com.ensar.clmp.marketing.domain.MarketingStatus.CLOSED;
import static com.ensar.clmp.marketing.domain.MarketingStatus.DRAFT;
import static com.ensar.clmp.marketing.domain.MarketingStatus.HOLD;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.ensar.clmp.auth.domain.Role;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.marketing.domain.MarketingAssignment;
import com.ensar.clmp.marketing.domain.MarketingStatus;

/**
 * FR-041 transition table with per-edge actors. "Owner" means the RECRUITER linked to the
 * consultant's current recruiter. Consultant-status guards live in MarketingService.
 */
@Component
public class MarketingTransitions {

    private enum Actors {
        ADMIN_OWNER, ADMIN_OWNER_MANAGER, ADMIN_MANAGER
    }

    private record Edge(MarketingStatus to, Actors actors, boolean reasonRequired) {
    }

    private static final Map<MarketingStatus, List<Edge>> EDGES = Map.of(
            DRAFT, List.of(new Edge(ACTIVE, Actors.ADMIN_OWNER, false),
                    new Edge(CLOSED, Actors.ADMIN_OWNER_MANAGER, false)),
            ACTIVE, List.of(new Edge(HOLD, Actors.ADMIN_OWNER_MANAGER, true),
                    new Edge(CLOSED, Actors.ADMIN_OWNER_MANAGER, true)),
            HOLD, List.of(new Edge(ACTIVE, Actors.ADMIN_OWNER_MANAGER, false),
                    new Edge(CLOSED, Actors.ADMIN_OWNER_MANAGER, true)),
            CLOSED, List.of(new Edge(ACTIVE, Actors.ADMIN_MANAGER, false)));

    /** Targets this caller may choose; excludes reopening an assignment closed by a placement. */
    public List<MarketingStatus> allowedFor(MarketingStatus from, String closeReason, Role role, boolean owner) {
        return EDGES.getOrDefault(from, List.of()).stream()
                .filter(e -> permits(e.actors(), role, owner))
                .filter(e -> !(from == CLOSED && MarketingAssignment.PLACED_REASON.equals(closeReason)))
                .map(Edge::to)
                .toList();
    }

    public void validate(MarketingStatus from, MarketingStatus to, String closeReason, String reason, Role role,
            boolean owner) {
        Edge edge = EDGES.getOrDefault(from, List.of()).stream().filter(e -> e.to() == to).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TRANSITION,
                        "Cannot change a " + from + " assignment to " + to + ".",
                        Map.of("currentStatus", from, "allowedTransitions", allowedFor(from, closeReason, role, owner))));
        if (!permits(edge.actors(), role, owner)) {
            throw new AccessDeniedException("Role may not perform this marketing transition");
        }
        if (from == CLOSED && MarketingAssignment.PLACED_REASON.equals(closeReason)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "An assignment closed because the consultant was placed cannot be reopened.");
        }
        if (edge.reasonRequired() && (reason == null || reason.isBlank())) {
            throw BusinessException.fieldError("reason", "A reason is required.");
        }
    }

    private static boolean permits(Actors actors, Role role, boolean owner) {
        Set<Role> roles = switch (actors) {
            case ADMIN_OWNER -> Set.of(Role.ADMIN);
            case ADMIN_OWNER_MANAGER -> Set.of(Role.ADMIN, Role.MANAGER);
            case ADMIN_MANAGER -> Set.of(Role.ADMIN, Role.MANAGER);
        };
        boolean ownerAllowed = actors != Actors.ADMIN_MANAGER;
        return roles.contains(role) || (ownerAllowed && role == Role.RECRUITER && owner);
    }
}
