package com.ensar.clmp.submission.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.ensar.clmp.submission.domain.SubmissionStatus;
import com.ensar.clmp.submission.service.SubmissionFilter;
import com.ensar.clmp.submission.service.SubmissionService;

/** Create, status progression, and notes only. There is deliberately no PUT (research R19-7). */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Map<String, String> SORTS = Map.of("submittedDate", "submittedDate", "status", "status",
            "billRate", "billRate");

    private final SubmissionService submissions;
    private final CurrentUserProvider currentUser;

    public SubmissionController(SubmissionService submissions, CurrentUserProvider currentUser) {
        this.submissions = submissions;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public PageResponse<SubmissionListItem> list(
            @RequestParam(name = "status", required = false) List<SubmissionStatus> statuses,
            @RequestParam(required = false) Long recruiterId, @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long clientId, @RequestParam(required = false) Long consultantId,
            @RequestParam(required = false) LocalDate submittedFrom,
            @RequestParam(required = false) LocalDate submittedTo, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) String sort) {
        SubmissionFilter filter = new SubmissionFilter(statuses, recruiterId, vendorId, clientId, consultantId,
                submittedFrom, submittedTo);
        return submissions.list(filter,
                PageRequests.of(page, size, sort, SORTS, Sort.by(Sort.Order.desc("createdAt"))), currentUser.get());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public SubmissionDetail get(@PathVariable Long id) {
        return submissions.getDetail(id, currentUser.get());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public SubmissionDetail create(@Valid @RequestBody SubmissionCreateRequest request) {
        return submissions.create(request, currentUser.get());
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public SubmissionDetail changeStatus(@PathVariable Long id, @Valid @RequestBody SubmissionStatusRequest request) {
        return submissions.changeStatus(id, request.targetStatus(), request.note(), request.submittedDate(),
                request.version(), currentUser.get());
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public NoteResponse addNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return submissions.addNote(id, request.body(), currentUser.get());
    }
}
