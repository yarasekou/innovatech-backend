package com.innovatech.api.controller;

import com.innovatech.api.dto.*;
import com.innovatech.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, setup")
public class AuthController {

    private final AuthService authService;

    // -----------------------------------------
    // REGISTER ADMIN + ENTREPRISE (self-service)
    // -----------------------------------------

    /**
     * POST /api/auth/register-admin
     * <p>
     * Crée un compte ADMIN et son entreprise en une transaction.
     * Retourne immédiatement un JWT.
     */
    @PostMapping("/register-admin")
    @Operation(
            summary = "Inscription d'un ADMIN + son entreprise",
            description = "Crée un compte ADMIN et son entreprise en une transaction. Retourne un JWT."
    )
    public ResponseEntity<AuthResponseDTO> registerAdmin(
            @Valid @RequestBody RegisterAdminRequestDTO dto) {
        AuthResponseDTO response = authService.registerAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -----------------------------------------
    // SETUP SUPER_ADMIN (bootstrap unique)
    // -----------------------------------------

    /**
     * POST /api/auth/setup-super-admin
     * <p>
     * Crée le tout premier SUPER_ADMIN de la plateforme.
     * <p>
     * Endpoint public, protégé uniquement par une clé secrète.
     * Ne fonctionne qu'une seule fois (tant qu'aucun SUPER_ADMIN n'existe).
     */
    @PostMapping("/setup-super-admin")
    @Operation(summary = "Créer le premier SUPER_ADMIN (à usage unique)")
    public ResponseEntity<AuthResponseDTO> setupSuperAdmin(
            @Valid @RequestBody SetupSuperAdminDTO dto) {
        AuthResponseDTO response = authService.setupSuperAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -----------------------------------------
    // LOGIN
    // -----------------------------------------

    /**
     * POST /api/auth/login
     */
    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    // -----------------------------------------
    // CHANGE PASSWORD
    // -----------------------------------------

    /**
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    @Operation(summary = "Changer mon mot de passe")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.changePassword(userDetails.getUsername(), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/profile")
    @Operation(summary = "Modifier mon profil")
    public ResponseEntity<AuthResponseDTO> updateProfile(
            @Valid @RequestBody UpdateProfileDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                authService.updateProfile(userDetails.getUsername(), dto));
    }
}