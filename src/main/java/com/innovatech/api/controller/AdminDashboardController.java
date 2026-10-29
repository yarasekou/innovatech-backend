package com.innovatech.api.controller;

import com.innovatech.api.dto.dashboard.DashboardStatsDTO;
import com.innovatech.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Dashboard", description = "Statistiques globales (SUPER_ADMIN)")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/global")
    @Operation(summary = "Statistiques globales de la plateforme")
    public ResponseEntity<DashboardStatsDTO> getGlobalStats() {
        return ResponseEntity.ok(dashboardService.getGlobalStats());
    }
}