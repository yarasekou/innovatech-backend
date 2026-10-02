package com.innovatech.api.controller;

import com.innovatech.api.dto.FactureRequestDTO;
import com.innovatech.api.dto.FactureResponseDTO;
import com.innovatech.api.service.FactureService;
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
@RequestMapping("/factures")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Factures", description = "Factures clients et cycle de vie")
public class FactureController {

    private final FactureService factureService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @Operation(summary = "Liste toutes les factures accessibles")
    public ResponseEntity<List<FactureResponseDTO>> getAll() {
        return ResponseEntity.ok(factureService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une facture")
    public ResponseEntity<FactureResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.findById(id));
    }

    @GetMapping("/numero/{numero}")
    @Operation(summary = "Détail d'une facture par numéro")
    public ResponseEntity<FactureResponseDTO> getByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(factureService.findByNumero(numero));
    }

    @GetMapping("/commande/{commandeId}")
    @Operation(summary = "Facture liée à une commande")
    public ResponseEntity<FactureResponseDTO> getByCommande(@PathVariable Long commandeId) {
        return ResponseEntity.ok(factureService.findByCommande(commandeId));
    }

    // -----------------------------------------
    // CREATE — USER uniquement
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Créer une facture BROUILLON pour une commande LIVREE")
    public ResponseEntity<FactureResponseDTO> create(@Valid @RequestBody FactureRequestDTO dto) {
        FactureResponseDTO created = factureService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // CYCLE DE VIE — USER uniquement
    // -----------------------------------------

    @PatchMapping("/{id}/emettre")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Émettre une facture (BROUILLON → EMISE)")
    public ResponseEntity<FactureResponseDTO> emettre(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.emettre(id));
    }

    @PatchMapping("/{id}/payer")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Marquer une facture comme payée")
    public ResponseEntity<FactureResponseDTO> payer(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.marquerPayee(id));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Annuler une facture (non payée)")
    public ResponseEntity<FactureResponseDTO> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.annuler(id));
    }

    // -----------------------------------------
    // BATCH — ADMIN uniquement
    // -----------------------------------------

    @PostMapping("/mettre-a-jour-retards")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch : passer en EN_RETARD les factures échues (ADMIN)")
    public ResponseEntity<Integer> mettreAJourRetards() {
        int count = factureService.mettreAJourFacturesEnRetard();
        return ResponseEntity.ok(count);
    }

    // -----------------------------------------
    // DELETE — ADMIN uniquement
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une facture BROUILLON (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        factureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}