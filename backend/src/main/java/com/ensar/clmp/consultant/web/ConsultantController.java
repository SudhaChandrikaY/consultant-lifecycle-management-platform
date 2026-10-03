package com.ensar.clmp.consultant.web;

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
import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.consultant.service.ConsultantFilter;
import com.ensar.clmp.consultant.service.ConsultantService;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.HistoryQueryService;
import com.ensar.clmp.history.web.HistoryEntry;
import com.ensar.clmp.reference.domain.VisaType;

@RestController
@RequestMapping("/api/consultants")
public class ConsultantController {

    private static final Map<String, String> SORTS = Map.of(
            "lastName", "lastName|firstName",
            "status", "status",
            "yearsExperience", "yearsExperience",
            "primarySkill", "primarySkill");
    private static final Sort DEFAULT_SORT = Sort.by("lastName", "firstName");

    private final ConsultantService consultants;
    private final HistoryQueryService historyQuery;
    private final CurrentUserProvider currentUser;

    public ConsultantController(ConsultantService consultants, HistoryQueryService historyQuery,
            CurrentUserProvider currentUser) {
        this.consultants = consultants;
        this.historyQuery = historyQuery;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER','HR_OPERATIONS')")
    public PageResponse<ConsultantListItem> list(@RequestParam(required = false) String q,
            @RequestParam(name = "status", required = false) List<ConsultantStatus> statuses,
            @RequestParam(required = false) String primarySkill, @RequestParam(required = false) VisaType visaType,
            @RequestParam(required = false) Long recruiterId,
            @RequestParam(required = false) Boolean needsReassignment,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        ConsultantFilter filter = new ConsultantFilter(q, statuses, primarySkill, visaType, recruiterId,
                needsReassignment);
        return consultants.list(filter, PageRequests.of(page, size, sort, SORTS, DEFAULT_SORT), currentUser.get());
    }

    @GetMapping("/skills")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER','HR_OPERATIONS')")
    public List<String> skills() {
        return consultants.distinctSkills();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER','HR_OPERATIONS')")
    public ConsultantDetail get(@PathVariable Long id) {
        return consultants.getDetail(id, currentUser.get());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR_OPERATIONS')")
    public ConsultantDetail create(@Valid @RequestBody ConsultantRequest request) {
        return consultants.create(request, currentUser.get());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_OPERATIONS')")
    public ConsultantDetail update(@PathVariable Long id, @Valid @RequestBody ConsultantRequest request) {
        return consultants.update(id, request, currentUser.get());
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','HR_OPERATIONS')")
    public ConsultantDetail changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return consultants.changeStatus(id, request.targetStatus(), request.reason(), request.version(),
                currentUser.get());
    }

    @PostMapping("/{id}/recruiter")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ConsultantDetail assignRecruiter(@PathVariable Long id, @Valid @RequestBody AssignRecruiterRequest request) {
        return consultants.assignRecruiter(id, request.recruiterId(), request.version(), currentUser.get());
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER','HR_OPERATIONS')")
    public PageResponse<HistoryEntry> history(@PathVariable Long id, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var user = currentUser.get();
        consultants.assertCanView(id, user);
        return PageResponse.from(historyQuery.forEntity(HistoryEntityType.CONSULTANT, id, user,
                PageRequests.of(page, size, null, Map.of(), Sort.unsorted())));
    }
}
