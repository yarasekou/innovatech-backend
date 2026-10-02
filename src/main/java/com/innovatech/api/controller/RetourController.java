package com.innovatech.api.controller;

import com.innovatech.api.dto.RetourRequestDTO;
import com.innovatech.api.dto.RetourResponseDTO;
import com.innovatech.api.entity.StatutRetour;
import com.innovatech.api.service.RetourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST pour la gestion des retours clients.
 * <p>
 * Toutes les routes nécessitent une authentification.
 * Le filtrage par propriétaire (patron / SELLER) est fait dans le service
 * via {@code commande.vendeur.id} (= dataOwnerId).
 */
@RestController
@RequestMapping("/retours")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Retours clients", description = "Gestion des retours clients")
public class RetourController {

    private final RetourService retourService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @Operation(summary = "Liste tous les retours accessibles à l'utilisateur connecté")
    public ResponseEntity<List<RetourResponseDTO>> findAll() {
        return ResponseEntity.ok(retourService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un retour par son id")
    public ResponseEntity<RetourResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(retourService.findById(id));
    }

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Liste les retours par statut")
    public ResponseEntity<List<RetourResponseDTO>> findByStatut(
            @PathVariable StatutRetour statut) {
        return ResponseEntity.ok(retourService.findByStatut(statut));
    }

    @GetMapping("/commande/{commandeId}")
    @Operation(summary = "Liste les retours liés à une commande")
    public ResponseEntity<List<RetourResponseDTO>> findByCommande(
            @PathVariable Long commandeId) {
        return ResponseEntity.ok(retourService.findByCommande(commandeId));
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @PostMapping
    @Operation(summary = "Créer une demande de retour pour une commande LIVREE")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RetourResponseDTO> create(
            @Valid @RequestBody RetourRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        RetourResponseDTO created = retourService.create(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // CHANGEMENT DE STATUT
    // -----------------------------------------

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d'un retour (transitions contrôlées)")
    public ResponseEntity<RetourResponseDTO> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutRetour statut) {
        return ResponseEntity.ok(retourService.changerStatut(id, statut));
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un retour (uniquement si non reçu/remboursé)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        retourService.delete(id);
        return ResponseEntity.noContent().build();
    }
}