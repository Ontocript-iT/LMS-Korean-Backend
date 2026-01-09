package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.dtos.AdminDashboardDTO;
import com.lms.ontocriptIT.backend.services.centralServices.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardDTO> getDashboardAnalysis() {
        return ResponseEntity.ok(dashboardService.getAdminAnalysis());
    }
}