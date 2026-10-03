package com.ensar.clmp.common.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ensar.clmp.common.error.BusinessException;

/**
 * Builds a {@link Pageable} from {@code page}/{@code size}/{@code sort} (research R14): default
 * size 25, maximum 100, and only allowlisted sort fields. The allowlist maps API names to entity
 * paths. A stable {@code id} tie-breaker is always appended.
 */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 25;
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable of(Integer page, Integer size, String sort, Map<String, String> sortAllowlist,
            Sort defaultSort) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0) {
            throw BusinessException.fieldError("page", "Page must be 0 or greater.");
        }
        if (s < 1 || s > MAX_SIZE) {
            throw BusinessException.fieldError("size", "Size must be between 1 and " + MAX_SIZE + ".");
        }
        Sort order = parseSort(sort, sortAllowlist, defaultSort).and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(p, s, order);
    }

    /** For unpaged queries (dashboards, embedded panels) that still need an allowlisted sort. */
    public static Sort parseSort(String sort, Map<String, String> allowlist, Sort defaultSort) {
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String path = allowlist.get(field);
        if (path == null) {
            throw BusinessException.fieldError("sort", "Sorting by '" + field + "' is not supported.");
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElseThrow(() -> BusinessException.fieldError("sort", "Sort direction must be asc or desc."));
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String p : path.split("\\|")) {
            orders.add(new Sort.Order(direction, p));
        }
        return Sort.by(orders);
    }
}
