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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/commandes-achat")
@RequiredArgsConstructor
@Tag(name = "Commandes d'achat", description = "Achats auprès des fournisseurs")
public class CommandeAchatController {

    private final CommandeAchatService commandeAchatService;

    @GetMapping
    @Operation(summary = "Liste toutes les commandes d'achat")
    public ResponseEntity<List<CommandeAchatResponseDTO>> getAll() {
        return ResponseEntity.ok(commandeAchatService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeAchatResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeAchatService.findById(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<CommandeAchatResponseDTO> getByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(commandeAchatService.findByNumero(numero));
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    public ResponseEntity<List<CommandeAchatResponseDTO>> getByFournisseur(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(commandeAchatService.findByFournisseur(fournisseurId));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<CommandeAchatResponseDTO>> getByStatut(@PathVariable StatutAchat statut) {
        return ResponseEntity.ok(commandeAchatService.findByStatut(statut));
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle commande d'achat")
    public ResponseEntity<CommandeAchatResponseDTO> create(
            @Valid @RequestBody CommandeAchatRequestDTO dto) {
        CommandeAchatResponseDTO created = commandeAchatService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommandeAchatResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CommandeAchatRequestDTO dto) {
        return ResponseEntity.ok(commandeAchatService.update(id, dto));
    }

    @PatchMapping("/{id}/statut/{nouveauStatut}")
    @Operation(summary = "Changer le statut (RECUE = stock augmenté)")
    public ResponseEntity<CommandeAchatResponseDTO> changerStatut(
            @PathVariable Long id,
            @PathVariable StatutAchat nouveauStatut) {
        return ResponseEntity.ok(commandeAchatService.changerStatut(id, nouveauStatut));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une commande d'achat (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandeAchatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}