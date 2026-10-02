package com.innovatech.api.controller;

import com.innovatech.api.dto.FournisseurRequestDTO;
import com.innovatech.api.dto.FournisseurResponseDTO;
import com.innovatech.api.service.FournisseurService;
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
@RequestMapping("/fournisseurs")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs", description = "Fournisseurs auprès desquels InnovaTech achète")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    // -----------------------------------------
    // READ — Tous les rôles authentifiés
    // -----------------------------------------

    @GetMapping
    @Operation(summary = "Liste tous les fournisseurs accessibles")
    public ResponseEntity<List<FournisseurResponseDTO>> getAll() {
        return ResponseEntity.ok(fournisseurService.findAll());
    }

    @GetMapping("/actifs")
    @Operation(summary = "Liste les fournisseurs actifs")
    public ResponseEntity<List<FournisseurResponseDTO>> getActifs() {
        return ResponseEntity.ok(fournisseurService.findAllActifs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un fournisseur")
    public ResponseEntity<FournisseurResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fournisseurService.findById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Détail d'un fournisseur par code")
    public ResponseEntity<FournisseurResponseDTO> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(fournisseurService.findByCode(code));
    }

    @GetMapping("/pays/{pays}")
    @Operation(summary = "Fournisseurs par pays")
    public ResponseEntity<List<FournisseurResponseDTO>> getByPays(@PathVariable String pays) {
        return ResponseEntity.ok(fournisseurService.findByPays(pays));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des fournisseurs")
    public ResponseEntity<List<FournisseurResponseDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(fournisseurService.search(query));
    }

    // -----------------------------------------
    // CREATE — USER uniquement
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Créer un nouveau fournisseur (USER uniquement)")
    public ResponseEntity<FournisseurResponseDTO> create(
            @Valid @RequestBody FournisseurRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        FournisseurResponseDTO created = fournisseurService.create(dto, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE — USER uniquement
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Modifier un fournisseur (USER uniquement)")
    public ResponseEntity<FournisseurResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FournisseurRequestDTO dto) {
        return ResponseEntity.ok(fournisseurService.update(id, dto));
    }

    // -----------------------------------------
    // DELETE — USER uniquement
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Supprimer un fournisseur (USER uniquement)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fournisseurService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Désactiver un fournisseur (USER uniquement)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        fournisseurService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}