package com.innovatech.api.controller;

import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.service.AdminService;
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
public class AdminController {

    private final AdminService adminService;

    /**
     * GET /api/admin/users — Liste tous les utilisateurs.
     */
    @GetMapping("/users")
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
}