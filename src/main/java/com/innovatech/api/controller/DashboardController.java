package com.innovatech.api.controller;

import com.innovatech.api.dto.dashboard.*;
import com.innovatech.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Dashboard", description = "Statistiques et indicateurs (ADMIN)")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Statistiques globales")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/top-produits")
    @Operation(summary = "Top N produits vendus")
    public ResponseEntity<List<TopProduitDTO>> getTopProduits(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getTopProduits(limit));
    }

    @GetMapping("/top-clients")
    @Operation(summary = "Top N clients par CA")
    public ResponseEntity<List<TopClientDTO>> getTopClients(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getTopClients(limit));
    }

    @GetMapping("/ca-mensuel")
    @Operation(summary = "Chiffre d'affaires mensuel")
    public ResponseEntity<List<ChiffreAffairesMoisDTO>> getChiffreAffairesMensuel() {
        return ResponseEntity.ok(dashboardService.getChiffreAffairesMensuel());
    }
}