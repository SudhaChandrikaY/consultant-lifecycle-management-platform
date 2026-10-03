package com.ensar.clmp.placement.web;

import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ensar.clmp.auth.CurrentUserProvider;
import com.ensar.clmp.common.web.PageRequests;
import com.ensar.clmp.common.web.PageResponse;
import com.ensar.clmp.placement.service.PlacementFilter;
import com.ensar.clmp.placement.service.PlacementService;

/** Placements workspace. HR_OPERATIONS has no access at all (FR-077). */
@RestController
@RequestMapping("/api/placements")
public class PlacementController {

    private static final Map<String, String> SORTS = Map.of("startDate", "startDate", "createdAt", "createdAt");

    private final PlacementService placements;
    private final CurrentUserProvider currentUser;

    public PlacementController(PlacementService placements, CurrentUserProvider currentUser) {
        this.placements = placements;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public PageResponse<PlacementListItem> list(@RequestParam(required = false) Long recruiterId,
            @RequestParam(required = false) Long clientId, @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) LocalDate startFrom, @RequestParam(required = false) LocalDate startTo,
            @RequestParam(required = false) LocalDate createdFrom, @RequestParam(required = false) LocalDate createdTo,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PlacementFilter filter = new PlacementFilter(recruiterId, clientId, vendorId, startFrom, startTo, createdFrom,
                createdTo);
        return placements.list(filter, PageRequests.of(page, size, sort, SORTS, Sort.by(Sort.Order.desc("createdAt"))),
                currentUser.get());
    }

    @GetMapping("/draft")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public PlacementDraft draft(@RequestParam Long submissionId) {
        return placements.draft(submissionId, currentUser.get());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public PlacementDetail get(@PathVariable Long id) {
        return placements.getDetail(id, currentUser.get());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public PlacementCreated create(@Valid @RequestBody PlacementCreateRequest request) {
        return placements.create(request, currentUser.get());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PlacementDetail edit(@PathVariable Long id, @Valid @RequestBody PlacementPatchRequest request) {
        return placements.edit(id, request, currentUser.get());
    }
}
