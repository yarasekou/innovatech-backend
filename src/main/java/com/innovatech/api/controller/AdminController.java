package com.innovatech.api.controller;

import com.innovatech.api.dto.AssignPatronDTO;
import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints d'administration (réservés ROLE_ADMIN).
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Gestion des utilisateurs (ADMIN)")
public class AdminController {

    private final AdminService adminService;

    // -----------------------------------------
    // CONSULTATION
    // -----------------------------------------

    @GetMapping("/users")
    @Operation(summary = "Liste tous les utilisateurs")

    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.findAllUsers());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Détail d'un utilisateur")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.findUserById(id));
    }

    // -----------------------------------------
    // GESTION DES RÔLES
    // -----------------------------------------

    @PatchMapping("/users/{id}/promote")
    @Operation(summary = "Promouvoir un user en ADMIN")
    public ResponseEntity<UserResponseDTO> promote(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.promoteToAdmin(id));
    }

    @PatchMapping("/users/{id}/demote")
    @Operation(summary = "Rétrograder un ADMIN en USER")
    public ResponseEntity<UserResponseDTO> demote(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.demoteToUser(id));
    }

    @PatchMapping("/users/{id}/promote-seller")
    @Operation(summary = "Promouvoir un user en SELLER (rôle unique)")
    public ResponseEntity<UserResponseDTO> promoteSeller(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.promoteToSeller(id));
    }

    @PatchMapping("/users/{id}/demote-seller")
    @Operation(summary = "Rétrograder un SELLER en USER")
    public ResponseEntity<UserResponseDTO> demoteSeller(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.demoteFromSeller(id));
    }

    @PatchMapping("/users/{id}/toggle-actif")
    @Operation(summary = "Activer/désactiver un utilisateur")
    public ResponseEntity<UserResponseDTO> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleActif(id));
    }

    // -----------------------------------------
    // HIÉRARCHIE : SELLER → PATRON
    // -----------------------------------------

    @PatchMapping("/users/{id}/assign-patron")
    @Operation(summary = "Rattacher un SELLER à un patron")
    public ResponseEntity<UserResponseDTO> assignPatron(
            @PathVariable Long id,
            @Valid @RequestBody AssignPatronDTO dto) {
        return ResponseEntity.ok(adminService.assignPatron(id, dto.getPatronId()));
    }

    @PatchMapping("/users/{id}/remove-patron")
    @Operation(summary = "Retirer le patron d'un SELLER")
    public ResponseEntity<UserResponseDTO> removePatron(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.removePatron(id));
    }

    @GetMapping("/users/{id}/employes")
    @Operation(summary = "Liste les SELLERs d'un patron")
    public ResponseEntity<List<UserResponseDTO>> getEmployes(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.findEmployes(id));
    }
}