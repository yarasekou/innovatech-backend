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
@Tag(name = "Fournisseurs", description = "Fournisseurs scopés par entreprise")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<FournisseurResponseDTO>> getAll() {
        return ResponseEntity.ok(fournisseurService.findAll());
    }

    @GetMapping("/actifs")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<FournisseurResponseDTO>> getActifs() {
        return ResponseEntity.ok(fournisseurService.findAllActifs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<FournisseurResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fournisseurService.findById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<FournisseurResponseDTO> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(fournisseurService.findByCode(code));
    }

    @GetMapping("/pays/{pays}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<FournisseurResponseDTO>> getByPays(@PathVariable String pays) {
        return ResponseEntity.ok(fournisseurService.findByPays(pays));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<FournisseurResponseDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(fournisseurService.search(query));
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Créer un fournisseur dans mon entreprise")
    public ResponseEntity<FournisseurResponseDTO> create(
            @Valid @RequestBody FournisseurRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        FournisseurResponseDTO created = fournisseurService.create(dto, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<FournisseurResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FournisseurRequestDTO dto) {
        return ResponseEntity.ok(fournisseurService.update(id, dto));
    }

    // -----------------------------------------
    // DELETE / DEACTIVATE
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fournisseurService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        fournisseurService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}