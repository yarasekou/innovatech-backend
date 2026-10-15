package com.innovatech.api.controller;

import com.innovatech.api.dto.CreateUserDTO;
import com.innovatech.api.dto.UpdateUserDTO;
import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des USERs (managers).
 * <ul>
 *   <li>ADMIN       → crée et gère SES USERs (dans son entreprise)</li>
 *   <li>SUPER_ADMIN → voit et gère TOUS les USERs (supervision plateforme)</li>
 * </ul>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")   // ← LE FIX
@Tag(name = "USERs", description = "Gestion des USERs par un ADMIN / SUPER_ADMIN")
public class UserController {

    private final UserService userService;

    // -----------------------------------------
    // CREATE — ADMIN uniquement (vérifié dans le service)
    // -----------------------------------------

    @PostMapping
    @Operation(summary = "Créer un USER (ADMIN uniquement)")
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody CreateUserDTO dto) {
        UserResponseDTO created = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @Operation(summary = "Liste mes USERs (ADMIN) ou tous (SUPER_ADMIN)")
    public ResponseEntity<List<UserResponseDTO>> getMyUsers() {
        return ResponseEntity.ok(userService.findMyUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un USER")
    public ResponseEntity<UserResponseDTO> getMyUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findMyUserById(id));
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un USER")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @PatchMapping("/{id}/toggle-actif")
    @Operation(summary = "Activer/désactiver un USER")
    public ResponseEntity<UserResponseDTO> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleActif(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer (soft) un USER")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}