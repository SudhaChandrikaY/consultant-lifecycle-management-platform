package com.ensar.clmp.reference.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.reference.service.CounterpartyService;

/** Vendor/client typeahead (at most 20, case-insensitive contains). Not available to HR_OPERATIONS. */
@RestController
public class CounterpartyController {

    private final CounterpartyService counterparties;

    public CounterpartyController(CounterpartyService counterparties) {
        this.counterparties = counterparties;
    }

    @GetMapping("/api/vendors")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public List<NamedRef> vendors(@RequestParam(required = false) String q) {
        return counterparties.searchVendors(q);
    }

    @GetMapping("/api/clients")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECRUITER')")
    public List<NamedRef> clients(@RequestParam(required = false) String q) {
        return counterparties.searchClients(q);
    }
}
