package com.ensar.clmp.recruiter.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ensar.clmp.auth.CurrentUserProvider;
import com.ensar.clmp.common.web.PageRequests;
import com.ensar.clmp.common.web.PageResponse;
import com.ensar.clmp.recruiter.domain.RecruiterStatus;
import com.ensar.clmp.recruiter.service.RecruiterFilter;
import com.ensar.clmp.recruiter.service.RecruiterService;

@RestController
@RequestMapping("/api/recruiters")
public class RecruiterController {

    private static final Map<String, String> SORTS = Map.of("fullName", "fullName", "status", "status");

    private final RecruiterService recruiters;
    private final CurrentUserProvider currentUser;

    public RecruiterController(RecruiterService recruiters, CurrentUserProvider currentUser) {
        this.recruiters = recruiters;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PageResponse<RecruiterListItem> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long teamId, @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) RecruiterStatus status, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) String sort) {
        return recruiters.list(new RecruiterFilter(q, teamId, regionId, status),
                PageRequests.of(page, size, sort, SORTS, Sort.by("fullName")));
    }

    @GetMapping("/linkable-users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LinkableUser> linkableUsers() {
        return recruiters.linkableUsers(currentUser.get());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public RecruiterDetail get(@PathVariable Long id) {
        return recruiters.getDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RecruiterDetail create(@Valid @RequestBody RecruiterRequest request) {
        return recruiters.create(request, currentUser.get());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RecruiterDetail update(@PathVariable Long id, @Valid @RequestBody RecruiterRequest request) {
        return recruiters.update(id, request, currentUser.get());
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public RecruiterDetail changeStatus(@PathVariable Long id, @Valid @RequestBody RecruiterStatusRequest request) {
        return recruiters.changeStatus(id, request.status(), request.confirm(), request.version(), currentUser.get());
    }
}
