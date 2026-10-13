package com.innovatech.api.controller;

import com.innovatech.api.dto.CommandeAchatRequestDTO;
import com.innovatech.api.dto.CommandeAchatResponseDTO;
import com.innovatech.api.entity.StatutAchat;
import com.innovatech.api.service.CommandeAchatService;
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

@RestController
@RequestMapping("/commandes-achat")
@RequiredArgsConstructor
@Tag(name = "Commandes d'achat", description = "Achats scopés par boutique")
public class CommandeAchatController {

    private final CommandeAchatService commandeAchatService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<CommandeAchatResponseDTO>> getAll() {
        return ResponseEntity.ok(commandeAchatService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<CommandeAchatResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeAchatService.findById(id));
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<CommandeAchatResponseDTO>> getByFournisseur(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(commandeAchatService.findByFournisseur(fournisseurId));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<CommandeAchatResponseDTO>> getByStatut(@PathVariable StatutAchat statut) {
        return ResponseEntity.ok(commandeAchatService.findByStatut(statut));
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Créer une nouvelle commande d'achat")
    public ResponseEntity<CommandeAchatResponseDTO> create(
            @Valid @RequestBody CommandeAchatRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommandeAchatResponseDTO created =
                commandeAchatService.create(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<CommandeAchatResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CommandeAchatRequestDTO dto) {
        return ResponseEntity.ok(commandeAchatService.update(id, dto));
    }

    // -----------------------------------------
    // STATUT
    // -----------------------------------------

    @PatchMapping("/{id}/statut/{nouveauStatut}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Changer le statut (RECUE = stock augmenté)")
    public ResponseEntity<CommandeAchatResponseDTO> changerStatut(
            @PathVariable Long id,
            @PathVariable StatutAchat nouveauStatut) {
        return ResponseEntity.ok(commandeAchatService.changerStatut(id, nouveauStatut));
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandeAchatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}