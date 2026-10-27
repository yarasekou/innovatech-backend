package com.innovatech.api.controller;

import com.innovatech.api.dto.FactureRequestDTO;
import com.innovatech.api.dto.FactureResponseDTO;
import com.innovatech.api.service.FactureService;
import com.innovatech.api.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/factures")
@RequiredArgsConstructor
@Tag(name = "Factures", description = "Factures scopées par boutique")
public class FactureController {

    private final FactureService factureService;
    private final PdfService pdfService;


    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<FactureResponseDTO>> getAll() {
        return ResponseEntity.ok(factureService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<FactureResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.findById(id));
    }

    @GetMapping("/commande/{commandeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Facture liée à une commande client")
    public ResponseEntity<FactureResponseDTO> getByCommande(@PathVariable Long commandeId) {
        return ResponseEntity.ok(factureService.findByCommande(commandeId));
    }

    // ⚠️ NOUVEAU : Pour les achats
    @GetMapping("/commande-achat/{commandeAchatId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Facture liée à une commande d'achat")
    public ResponseEntity<FactureResponseDTO> getByCommandeAchat(@PathVariable Long commandeAchatId) {
        return ResponseEntity.ok(factureService.findByCommandeAchat(commandeAchatId));
    }

    // -----------------------------------------
    // CREATE — USER / SELLER uniquement
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Créer une facture BROUILLON")
    public ResponseEntity<FactureResponseDTO> create(@Valid @RequestBody FactureRequestDTO dto) {
        FactureResponseDTO created = factureService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // CYCLE DE VIE — USER / SELLER
    // -----------------------------------------

    @PatchMapping("/{id}/emettre")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<FactureResponseDTO> emettre(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.emettre(id));
    }

    @PatchMapping("/{id}/payer")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<FactureResponseDTO> payer(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.marquerPayee(id));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<FactureResponseDTO> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.annuler(id));
    }

    // -----------------------------------------
    // BATCH — ADMIN
    // -----------------------------------------

    @PostMapping("/mettre-a-jour-retards")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch : passer en EN_RETARD les factures échues")
    public ResponseEntity<Integer> mettreAJourRetards() {
        return ResponseEntity.ok(factureService.mettreAJourFacturesEnRetard());
    }

    // -----------------------------------------
    // DELETE — USER / SELLER
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        factureService.delete(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Télécharger la facture en PDF")
    public ResponseEntity<InputStreamResource> downloadPdf(@PathVariable Long id) {
        // On délègue tout au service (qui gère la transaction)
        ByteArrayInputStream bis = factureService.generatePdf(id);

        // On récupère juste le numéro pour nommer le fichier
        FactureResponseDTO factureDto = factureService.findById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=facture-" + factureDto.getNumero() + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}