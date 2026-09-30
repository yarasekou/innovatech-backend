package com.innovatech.api.controller;

import com.innovatech.api.dto.CreateSellerDTO;
import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.service.SellerService;
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
 * Endpoints pour qu'un USER gère SES SELLERs.
 * <p>
 * ⚠️ Accès réservé aux USERs (pas SELLER, pas ADMIN).
 */
@RestController
@RequestMapping("/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "SELLERs (USER)", description = "Gestion des SELLERs par un USER")
public class SellerController {

    private final SellerService sellerService;

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @PostMapping
    @Operation(summary = "Créer un nouveau SELLER (rattaché à moi)")
    public ResponseEntity<UserResponseDTO> createSeller(
            @Valid @RequestBody CreateSellerDTO dto) {
        UserResponseDTO created = sellerService.createSeller(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @Operation(summary = "Liste mes SELLERs")
    public ResponseEntity<List<UserResponseDTO>> getMySellers() {
        return ResponseEntity.ok(sellerService.findMySellers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un de mes SELLERs")
    public ResponseEntity<UserResponseDTO> getMySeller(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.findMySellerById(id));
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @PatchMapping("/{id}/toggle-actif")
    @Operation(summary = "Activer/désactiver un SELLER")
    public ResponseEntity<UserResponseDTO> toggleSellerActif(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.toggleSellerActif(id));
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer définitivement un SELLER")
    public ResponseEntity<Void> deleteSeller(@PathVariable Long id) {
        sellerService.deleteSeller(id);
        return ResponseEntity.noContent().build();
    }
}