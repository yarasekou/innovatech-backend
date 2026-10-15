package com.innovatech.api.controller;

import com.innovatech.api.dto.EntrepriseRequestDTO;
import com.innovatech.api.dto.EntrepriseResponseDTO;
import com.innovatech.api.service.EntrepriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/entreprises")
@RequiredArgsConstructor
@Tag(name = "Entreprises", description = "Gestion de l'entreprise du USER connecté")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping("/me")
    @Operation(summary = "Mon entreprise")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<EntrepriseResponseDTO> getMyEntreprise() {
        return ResponseEntity.ok(entrepriseService.findMyEntreprise());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une entreprise (SUPER_ADMIN ou membre)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<EntrepriseResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(entrepriseService.findById(id));
    }
    // -----------------------------------------
    // UPDATE — ADMIN (owner) uniquement
    // ⚠️ Plus de POST : la création se fait à l'inscription via /auth/register-admin
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier mon entreprise")
    public ResponseEntity<EntrepriseResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EntrepriseRequestDTO dto) {
        return ResponseEntity.ok(entrepriseService.update(id, dto));
    }

    // -----------------------------------------
    // LOGO — ADMIN uniquement
    // -----------------------------------------

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Uploader / remplacer le logo de mon entreprise")
    public ResponseEntity<EntrepriseResponseDTO> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(entrepriseService.uploadLogo(file));
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer le logo de mon entreprise")
    public ResponseEntity<EntrepriseResponseDTO> deleteLogo() {
        return ResponseEntity.ok(entrepriseService.deleteLogo());
    }

    // -----------------------------------------
    // DELETE — SUPER_ADMIN uniquement
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Supprimer une entreprise (SUPER_ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entrepriseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}