package com.innovatech.api.controller;

import com.innovatech.api.dto.CommandeRequestDTO;
import com.innovatech.api.dto.CommandeResponseDTO;
import com.innovatech.api.entity.StatutCommande;
import com.innovatech.api.service.CommandeService;
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
@RequestMapping("/commandes")
@RequiredArgsConstructor
@Tag(name = "Commandes", description = "Commandes clients scopées par boutique")
public class CommandeController {

    private final CommandeService commandeService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Liste les commandes accessibles à l'utilisateur connecté")
    public ResponseEntity<List<CommandeResponseDTO>> getAll() {
        return ResponseEntity.ok(commandeService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Détail d'une commande")
    public ResponseEntity<CommandeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.findById(id));
    }

    // ⚠️ Endpoint supprimé : le numéro n'est unique que par (entreprise, user)
    // → utiliser /commandes/{id} à la place

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Commandes d'un client de la boutique")
    public ResponseEntity<List<CommandeResponseDTO>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(commandeService.findByClient(clientId));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Commandes par statut")
    public ResponseEntity<List<CommandeResponseDTO>> getByStatut(@PathVariable StatutCommande statut) {
        return ResponseEntity.ok(commandeService.findByStatut(statut));
    }

    // -----------------------------------------
    // CREATE — USER / SELLER uniquement
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Créer une nouvelle commande dans ma boutique")
    public ResponseEntity<CommandeResponseDTO> create(
            @Valid @RequestBody CommandeRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        CommandeResponseDTO created = commandeService.create(dto, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE — USER / SELLER uniquement
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Modifier une commande EN_ATTENTE de ma boutique")
    public ResponseEntity<CommandeResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CommandeRequestDTO dto) {
        return ResponseEntity.ok(commandeService.update(id, dto));
    }

    // -----------------------------------------
    // STATUT — USER / SELLER uniquement
    // -----------------------------------------

    @PatchMapping("/{id}/statut/{nouveauStatut}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Changer le statut d'une commande")
    public ResponseEntity<CommandeResponseDTO> changerStatut(
            @PathVariable Long id,
            @PathVariable StatutCommande nouveauStatut) {
        return ResponseEntity.ok(commandeService.changerStatut(id, nouveauStatut));
    }

    // -----------------------------------------
    // DELETE — USER / SELLER uniquement
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Supprimer une commande (non démarrée)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}