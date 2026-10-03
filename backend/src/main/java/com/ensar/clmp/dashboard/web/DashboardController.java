package com.ensar.clmp.dashboard.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ensar.clmp.auth.CurrentUserProvider;
import com.ensar.clmp.dashboard.service.DashboardService;

@RestController
public class DashboardController {

    private final DashboardService dashboard;
    private final CurrentUserProvider currentUser;

    public DashboardController(DashboardService dashboard, CurrentUserProvider currentUser) {
        this.dashboard = dashboard;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/dashboard")
    @PreAuthorize("isAuthenticated()")
    public DashboardResponse dashboard() {
        return dashboard.dashboard(currentUser.get());
    }
}
