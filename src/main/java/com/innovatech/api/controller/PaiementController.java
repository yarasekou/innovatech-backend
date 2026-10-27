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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paiements")
@RequiredArgsConstructor
@Tag(name = "Paiements", description = "Paiements scopés par boutique")
public class PaiementController {

    private final PaiementService paiementService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Liste les paiements accessibles")
    public ResponseEntity<List<PaiementResponseDTO>> getAll() {
        return ResponseEntity.ok(paiementService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<PaiementResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paiementService.findById(id));
    }

    @GetMapping("/commande/{commandeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Paiements d'une commande")
    public ResponseEntity<List<PaiementResponseDTO>> getByCommande(@PathVariable Long commandeId) {
        return ResponseEntity.ok(paiementService.findByCommande(commandeId));
    }

    @GetMapping("/commande/{commandeId}/reste")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Reste à payer d'une commande")
    public ResponseEntity<ResteAPayerDTO> getReste(@PathVariable Long commandeId) {
        return ResponseEntity.ok(paiementService.calculerReste(commandeId));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<PaiementResponseDTO>> getByStatut(@PathVariable StatutPaiement statut) {
        return ResponseEntity.ok(paiementService.findByStatut(statut));
    }

    // -----------------------------------------
    // CREATE — USER / SELLER uniquement
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Enregistrer un paiement")
    public ResponseEntity<PaiementResponseDTO> create(
            @Valid @RequestBody PaiementRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        PaiementResponseDTO created = paiementService.create(dto, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // STATUT — USER / SELLER uniquement
    // -----------------------------------------

    @PatchMapping("/{id}/statut/{nouveauStatut}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<PaiementResponseDTO> changerStatut(
            @PathVariable Long id,
            @PathVariable StatutPaiement nouveauStatut) {
        return ResponseEntity.ok(paiementService.changerStatut(id, nouveauStatut));
    }

    // -----------------------------------------
    // DELETE — USER / SELLER uniquement
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Supprimer un paiement non validé")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paiementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/achat/{commandeAchatId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Paiements d'une commande d'achat")
    public ResponseEntity<List<PaiementResponseDTO>> getByCommandeAchat(@PathVariable Long commandeAchatId) {
        return ResponseEntity.ok(paiementService.findByCommandeAchat(commandeAchatId));
    }
}