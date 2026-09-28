package com.innovatech.api.controller;

import com.innovatech.api.dto.PaiementRequestDTO;
import com.innovatech.api.dto.PaiementResponseDTO;
import com.innovatech.api.dto.ResteAPayerDTO;
import com.innovatech.api.entity.StatutPaiement;
import com.innovatech.api.service.PaiementService;
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
@RequestMapping("/paiements")
@RequiredArgsConstructor
@Tag(name = "Paiements", description = "Paiements reçus pour les commandes")
public class PaiementController {

    private final PaiementService paiementService;

    @GetMapping
    @Operation(summary = "Liste tous les paiements")
    public ResponseEntity<List<PaiementResponseDTO>> getAll() {
        return ResponseEntity.ok(paiementService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaiementResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paiementService.findById(id));
    }

    @GetMapping("/commande/{commandeId}")
    @Operation(summary = "Liste les paiements d'une commande")
    public ResponseEntity<List<PaiementResponseDTO>> getByCommande(@PathVariable Long commandeId) {
        return ResponseEntity.ok(paiementService.findByCommande(commandeId));
    }

    @GetMapping("/commande/{commandeId}/reste")
    @Operation(summary = "Reste à payer d'une commande")
    public ResponseEntity<ResteAPayerDTO> getReste(@PathVariable Long commandeId) {
        return ResponseEntity.ok(paiementService.calculerReste(commandeId));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<PaiementResponseDTO>> getByStatut(@PathVariable StatutPaiement statut) {
        return ResponseEntity.ok(paiementService.findByStatut(statut));
    }

    @PostMapping
    @Operation(summary = "Enregistrer un nouveau paiement")
    public ResponseEntity<PaiementResponseDTO> create(
            @Valid @RequestBody PaiementRequestDTO dto) {
        PaiementResponseDTO created = paiementService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/statut/{nouveauStatut}")
    public ResponseEntity<PaiementResponseDTO> changerStatut(
            @PathVariable Long id,
            @PathVariable StatutPaiement nouveauStatut) {
        return ResponseEntity.ok(paiementService.changerStatut(id, nouveauStatut));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un paiement (ADMIN uniquement)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paiementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}