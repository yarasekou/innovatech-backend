package com.innovatech.api.controller;

import com.innovatech.api.dto.ProduitRequestDTO;
import com.innovatech.api.dto.ProduitResponseDTO;
import com.innovatech.api.service.ProduitService;
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
@RequestMapping("/produits")
@RequiredArgsConstructor
@Tag(name = "Produits", description = "Produits scopés par entreprise")
public class ProduitController {

    private final ProduitService produitService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<ProduitResponseDTO>> getAll() {
        return ResponseEntity.ok(produitService.findAll());
    }

    @GetMapping("/actifs")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<ProduitResponseDTO>> getActifs() {
        return ResponseEntity.ok(produitService.findAllActifs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<ProduitResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.findById(id));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<ProduitResponseDTO> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(produitService.findByReference(reference));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<ProduitResponseDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(produitService.search(query));
    }

    @GetMapping("/stock-faible")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<ProduitResponseDTO>> getStockFaible(
            @RequestParam(value = "seuil", defaultValue = "10") Integer seuil) {
        return ResponseEntity.ok(produitService.findStockFaible(seuil));
    }

    @GetMapping("/categorie/{categorieId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<ProduitResponseDTO>> getByCategorie(
            @PathVariable Long categorieId,
            @RequestParam(value = "deep", defaultValue = "false") boolean deep) {
        return ResponseEntity.ok(produitService.findByCategorie(categorieId, deep));
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Créer un produit dans mon entreprise")
    public ResponseEntity<ProduitResponseDTO> create(
            @Valid @RequestBody ProduitRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        ProduitResponseDTO created = produitService.create(dto, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<ProduitResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ProduitRequestDTO dto) {
        return ResponseEntity.ok(produitService.update(id, dto));
    }

    // -----------------------------------------
    // DELETE / DEACTIVATE
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        produitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        produitService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}