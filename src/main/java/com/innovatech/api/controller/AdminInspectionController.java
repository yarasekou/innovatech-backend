package com.innovatech.api.controller;

import com.innovatech.api.dto.*;
import com.innovatech.api.service.AdminInspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin — Inspection", description = "Supervision des boutiques (SUPER_ADMIN)")
public class AdminInspectionController {

    private final AdminInspectionService adminInspectionService;

    // -----------------------------------------
    // 1. Profil
    // -----------------------------------------

    @GetMapping("/{userId}/profil")
    @Operation(summary = "Profil du user ciblé")
    public ResponseEntity<UserResponseDTO> getProfil(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getProfil(userId));
    }

    // -----------------------------------------
    // 2. USERs
    // -----------------------------------------

    @GetMapping("/{userId}/users")
    @Operation(summary = "USERs de l'entreprise du user ciblé")
    public ResponseEntity<List<UserResponseDTO>> getUsers(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getUsers(userId));
    }

    // -----------------------------------------
    // 3. Catégories
    // -----------------------------------------

    @GetMapping("/{userId}/categories")
    @Operation(summary = "Catégories de l'entreprise du user ciblé")
    public ResponseEntity<List<CategorieResponseDTO>> getCategories(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getCategories(userId));
    }

    // -----------------------------------------
    // 4. Produits
    // -----------------------------------------

    @GetMapping("/{userId}/produits")
    @Operation(summary = "Produits de l'entreprise du user ciblé")
    public ResponseEntity<List<ProduitResponseDTO>> getProduits(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getProduits(userId));
    }

    // -----------------------------------------
    // 5. Clients
    // -----------------------------------------

    @GetMapping("/{userId}/clients")
    @Operation(summary = "Clients de l'entreprise du user ciblé")
    public ResponseEntity<List<ClientResponseDTO>> getClients(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getClients(userId));
    }

    // -----------------------------------------
    // 6. Fournisseurs  ← NOUVEAU
    // -----------------------------------------

    @GetMapping("/{userId}/fournisseurs")
    @Operation(summary = "Fournisseurs de l'entreprise du user ciblé")
    public ResponseEntity<List<FournisseurResponseDTO>> getFournisseurs(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getFournisseurs(userId));
    }

    // -----------------------------------------
    // 7. Commandes
    // -----------------------------------------

    @GetMapping("/{userId}/commandes")
    @Operation(summary = "Commandes de l'entreprise du user ciblé")
    public ResponseEntity<List<CommandeResponseDTO>> getCommandes(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getCommandes(userId));
    }

    // -----------------------------------------
    // 8. Paiements
    // -----------------------------------------

    @GetMapping("/{userId}/paiements")
    @Operation(summary = "Paiements de l'entreprise du user ciblé")
    public ResponseEntity<List<PaiementResponseDTO>> getPaiements(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getPaiements(userId));
    }

    // -----------------------------------------
    // 9. Retours
    // -----------------------------------------

    @GetMapping("/{userId}/retours")
    @Operation(summary = "Retours de l'entreprise du user ciblé")
    public ResponseEntity<List<RetourResponseDTO>> getRetours(@PathVariable Long userId) {
        return ResponseEntity.ok(adminInspectionService.getRetours(userId));
    }
}