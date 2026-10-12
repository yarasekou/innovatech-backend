package com.innovatech.api.controller;

import com.innovatech.api.dto.EntrepriseRequestDTO;
import com.innovatech.api.dto.EntrepriseResponseDTO;
import com.innovatech.api.service.EntrepriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/entreprises")
@RequiredArgsConstructor
@Tag(name = "Entreprises", description = "Gestion de l'entreprise du USER")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping("/me")
    @Operation(summary = "Mon entreprise (USER ou SELLER → entreprise du patron)")
    public ResponseEntity<EntrepriseResponseDTO> getMyEntreprise() {
        return ResponseEntity.ok(entrepriseService.findMyEntreprise());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une entreprise (ADMIN ou propriétaire)")
    public ResponseEntity<EntrepriseResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(entrepriseService.findById(id));
    }

    // -----------------------------------------
    // CREATE — USER uniquement
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Créer mon entreprise (une seule par USER)")
    public ResponseEntity<EntrepriseResponseDTO> create(
            @Valid @RequestBody EntrepriseRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        EntrepriseResponseDTO created =
                entrepriseService.create(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE — USER uniquement
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Modifier mon entreprise")
    public ResponseEntity<EntrepriseResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EntrepriseRequestDTO dto) {
        return ResponseEntity.ok(entrepriseService.update(id, dto));
    }

    // -----------------------------------------
    // LOGO
    // -----------------------------------------

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Uploader / remplacer le logo de mon entreprise")
    public ResponseEntity<EntrepriseResponseDTO> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(entrepriseService.uploadLogo(file));
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Supprimer le logo de mon entreprise")
    public ResponseEntity<EntrepriseResponseDTO> deleteLogo() {
        return ResponseEntity.ok(entrepriseService.deleteLogo());
    }

    // -----------------------------------------
    // DELETE — ADMIN uniquement
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une entreprise (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entrepriseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}