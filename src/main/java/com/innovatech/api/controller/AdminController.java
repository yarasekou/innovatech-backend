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
 * Endpoints d'administration plateforme — réservés ROLE_SUPER_ADMIN.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Administration", description = "Gestion plateforme (SUPER_ADMIN uniquement)")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Liste tous les utilisateurs de la plateforme")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.findAllUsers());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Détail d'un utilisateur")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.findUserById(id));
    }

    @PatchMapping("/users/{id}/promote-super-admin")
    @Operation(summary = "Promouvoir un user en SUPER_ADMIN")
    public ResponseEntity<UserResponseDTO> promoteSuperAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.promoteToSuperAdmin(id));
    }

    @PatchMapping("/users/{id}/demote-super-admin")
    @Operation(summary = "Rétrograder un SUPER_ADMIN")
    public ResponseEntity<UserResponseDTO> demoteSuperAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.demoteFromSuperAdmin(id));
    }

    @PatchMapping("/users/{id}/toggle-actif")
    @Operation(summary = "Activer/désactiver un utilisateur")
    public ResponseEntity<UserResponseDTO> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleActif(id));
    }

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