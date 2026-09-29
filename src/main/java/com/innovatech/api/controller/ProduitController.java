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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produits")
@RequiredArgsConstructor
@Tag(name = "Produits", description = "Catalogue des produits et services")
public class ProduitController {

    private final ProduitService produitService;

    @GetMapping
    @Operation(summary = "Liste tous les produits")
    public ResponseEntity<List<ProduitResponseDTO>> getAll() {
        return ResponseEntity.ok(produitService.findAll());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<ProduitResponseDTO>> getActifs() {
        return ResponseEntity.ok(produitService.findAllActifs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProduitResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.findById(id));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<ProduitResponseDTO> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(produitService.findByReference(reference));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProduitResponseDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(produitService.search(query));
    }

    @GetMapping("/stock-faible")
    public ResponseEntity<List<ProduitResponseDTO>> getStockFaible(
            @RequestParam(value = "seuil", defaultValue = "10") Integer seuil) {
        return ResponseEntity.ok(produitService.findStockFaible(seuil));
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau produit")
    public ResponseEntity<ProduitResponseDTO> create(
            @Valid @RequestBody ProduitRequestDTO dto) {
        ProduitResponseDTO created = produitService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProduitResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ProduitRequestDTO dto) {
        return ResponseEntity.ok(produitService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un produit (ADMIN uniquement)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        produitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        produitService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Filtre par catégorie (avec option descendants).
     */
    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<List<ProduitResponseDTO>> getByCategorie(
            @PathVariable Long categorieId,
            @RequestParam(value = "deep", defaultValue = "false") boolean deep) {
        return ResponseEntity.ok(produitService.findByCategorie(categorieId, deep));
    }
}