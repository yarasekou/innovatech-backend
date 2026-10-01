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
@Tag(name = "Commandes", description = "Commandes clients et cycle de vie")
public class CommandeController {

    private final CommandeService commandeService;

    @GetMapping
    @Operation(summary = "Liste toutes les commandes")
    public ResponseEntity<List<CommandeResponseDTO>> getAll() {
        return ResponseEntity.ok(commandeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.findById(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<CommandeResponseDTO> getByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(commandeService.findByNumero(numero));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<CommandeResponseDTO>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(commandeService.findByClient(clientId));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<CommandeResponseDTO>> getByStatut(@PathVariable StatutCommande statut) {
        return ResponseEntity.ok(commandeService.findByStatut(statut));
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle commande")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommandeResponseDTO> create(
            @Valid @RequestBody CommandeRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String vendeurEmail = userDetails.getUsername();
        CommandeResponseDTO created = commandeService.create(dto, vendeurEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommandeResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CommandeRequestDTO dto) {
        return ResponseEntity.ok(commandeService.update(id, dto));
    }

    @PatchMapping("/{id}/statut/{nouveauStatut}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER', 'ADMIN')")
    public ResponseEntity<CommandeResponseDTO> changerStatut(
            @PathVariable Long id,
            @PathVariable StatutCommande nouveauStatut) {
        return ResponseEntity.ok(commandeService.changerStatut(id, nouveauStatut));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une commande (ADMIN uniquement)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}