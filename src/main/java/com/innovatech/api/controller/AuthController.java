package com.innovatech.api.controller;

import com.innovatech.api.dto.AuthResponseDTO;
import com.innovatech.api.dto.LoginRequestDTO;
import com.innovatech.api.dto.RegisterRequestDTO;
import com.innovatech.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.innovatech.api.dto.SetupAdminDTO;
import com.innovatech.api.dto.UserResponseDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion des utilisateurs")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @Operation(
            summary = "Inscription d'un nouvel utilisateur",
            description = "Crée un compte et retourne un token JWT immédiatement"
    )
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        AuthResponseDTO response = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     */
    @PostMapping("/login")
    @Operation(
            summary = "Connexion utilisateur",
            description = "Vérifie les identifiants et retourne un token JWT"
    )
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/setup-admin")
    public ResponseEntity<UserResponseDTO> setupFirstAdmin(
            @Valid @RequestBody SetupAdminDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(authService.setupFirstAdmin(dto, userDetails.getUsername()));
    }
}