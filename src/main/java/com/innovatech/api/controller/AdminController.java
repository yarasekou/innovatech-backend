package com.innovatech.api.controller;

import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    /**
     * GET /api/admin/users — Liste tous les utilisateurs.
     */
    @GetMapping("/users")
    @Operation(summary = "Liste tous les admins")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.findAllUsers());
    }

    /**
     * GET /api/admin/users/{id} — Détail d'un utilisateur.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.findUserById(id));
    }

    /**
     * PATCH /api/admin/users/{id}/promote — Promeut un user en ADMIN.
     */
    @PatchMapping("/users/{id}/promote")
    public ResponseEntity<UserResponseDTO> promote(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.promoteToAdmin(id));
    }

    /**
     * PATCH /api/admin/users/{id}/demote — Rétrograde un ADMIN en USER.
     */
    @PatchMapping("/users/{id}/demote")
    public ResponseEntity<UserResponseDTO> demote(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.demoteToUser(id));
    }

    /**
     * PATCH /api/admin/users/{id}/toggle-actif — Active/désactive un user.
     */
    @PatchMapping("/users/{id}/toggle-actif")
    public ResponseEntity<UserResponseDTO> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleActif(id));
    }

    /**
     * PATCH /api/admin/users/{id}/promote-seller
     * Promeut un utilisateur en SELLER.
     */
    @PatchMapping("/users/{id}/promote-seller")
    @Operation(summary = "Promouvoir un user en SELLER")
    public ResponseEntity<UserResponseDTO> promoteSeller(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.promoteToSeller(id));
    }

    /**
     * PATCH /api/admin/users/{id}/demote-seller
     * Retire le rôle SELLER.
     */
    @PatchMapping("/users/{id}/demote-seller")
    @Operation(summary = "Rétrograder un SELLER")
    public ResponseEntity<UserResponseDTO> demoteSeller(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.demoteFromSeller(id));
    }
}