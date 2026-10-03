package com.ensar.clmp.reference.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ensar.clmp.reference.service.ReferenceService;

/** Reference lists for forms and filters; open to every authenticated role. */
@RestController
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/api/reference")
    @PreAuthorize("isAuthenticated()")
    public ReferenceResponse reference() {
        return referenceService.reference();
    }
}
