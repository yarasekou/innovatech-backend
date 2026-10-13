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
 * Gestion des USERs (managers) par un ADMIN.
 * <p>
 * ⚠️ Réservé aux ADMINs.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "USERs (ADMIN)", description = "Gestion des USERs par un ADMIN")
public class UserController {

    private final UserService userService;

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @PostMapping
    @Operation(summary = "Créer un USER (manager) dans mon entreprise")
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody CreateUserDTO dto) {
        UserResponseDTO created = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @Operation(summary = "Liste mes USERs")
    public ResponseEntity<List<UserResponseDTO>> getMyUsers() {
        return ResponseEntity.ok(userService.findMyUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un de mes USERs")
    public ResponseEntity<UserResponseDTO> getMyUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findMyUserById(id));
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un de mes USERs")
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
}