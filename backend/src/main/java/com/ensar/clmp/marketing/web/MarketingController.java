package com.ensar.clmp.marketing.web;

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
import com.ensar.clmp.common.web.NoteRequest;
import com.ensar.clmp.common.web.NoteResponse;
import com.ensar.clmp.common.web.PageRequests;
import com.ensar.clmp.common.web.PageResponse;
import com.ensar.clmp.history.web.HistoryEntry;
import com.ensar.clmp.marketing.domain.MarketingStatus;
import com.ensar.clmp.marketing.service.MarketingFilter;
import com.ensar.clmp.marketing.service.MarketingService;

@RestController
@RequestMapping("/api/marketing-assignments")
public class MarketingController {

    private static final Map<String, String> SORTS = Map.of("targetDate", "targetDate", "startDate", "startDate",
            "status", "status");

    private final MarketingService marketing;
    private final CurrentUserProvider currentUser;

    public MarketingController(MarketingService marketing, CurrentUserProvider currentUser) {
        this.marketing = marketing;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public PageResponse<MarketingListItem> list(
            @RequestParam(name = "status", required = false) List<MarketingStatus> statuses,
            @RequestParam(required = false) Long recruiterId, @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Boolean overdue, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) String sort) {
        return marketing.list(new MarketingFilter(statuses, recruiterId, teamId, overdue),
                PageRequests.of(page, size, sort, SORTS, Sort.by("targetDate")), currentUser.get());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public MarketingDetail get(@PathVariable Long id) {
        return marketing.getDetail(id, currentUser.get());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public MarketingDetail create(@Valid @RequestBody MarketingCreateRequest request) {
        return marketing.create(request, currentUser.get());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public MarketingDetail updateDates(@PathVariable Long id, @Valid @RequestBody MarketingDatesRequest request) {
        return marketing.updateDates(id, request.startDate(), request.targetDate(), request.version(),
                currentUser.get());
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public MarketingDetail transition(@PathVariable Long id,
            @Valid @RequestBody MarketingTransitionRequest request) {
        return marketing.transition(id, request.targetStatus(), request.reason(), request.version(),
                currentUser.get());
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public NoteResponse addNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return marketing.addNote(id, request.body(), currentUser.get());
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public PageResponse<HistoryEntry> history(@PathVariable Long id, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.from(marketing.history(id, currentUser.get(),
                PageRequests.of(page, size, null, Map.of(), Sort.unsorted())));
    }
}
