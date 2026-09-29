package com.innovatech.api.controller;

import com.innovatech.api.dto.CategorieRequestDTO;
import com.innovatech.api.dto.CategorieResponseDTO;
import com.innovatech.api.dto.CategorieTreeDTO;
import com.innovatech.api.service.CategorieService;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Catégories", description = "Catégories hiérarchiques des produits")
public class CategorieController {

    private final CategorieService categorieService;

    @GetMapping
    @Operation(summary = "Liste toutes les catégories (flat)")
    public ResponseEntity<List<CategorieResponseDTO>> getAll() {
        return ResponseEntity.ok(categorieService.findAll());
    }

    @GetMapping("/racines")
    @Operation(summary = "Liste les catégories racines")
    public ResponseEntity<List<CategorieResponseDTO>> getRacines() {
        return ResponseEntity.ok(categorieService.findRacines());
    }

    @GetMapping("/tree")
    @Operation(summary = "Arbre complet des catégories")
    public ResponseEntity<List<CategorieTreeDTO>> getTree() {
        return ResponseEntity.ok(categorieService.getTree());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorieResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findById(id));
    }

    @GetMapping("/{id}/tree")
    @Operation(summary = "Arbre à partir d'une catégorie")
    public ResponseEntity<CategorieTreeDTO> getTreeFrom(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.getTreeFrom(id));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Enfants directs d'une catégorie")
    public ResponseEntity<List<CategorieResponseDTO>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findByParent(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategorieResponseDTO> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categorieService.findBySlug(slug));
    }

    @PostMapping
    @Operation(summary = "Créer une catégorie")
    public ResponseEntity<CategorieResponseDTO> create(
            @Valid @RequestBody CategorieRequestDTO dto) {
        CategorieResponseDTO created = categorieService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategorieResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CategorieRequestDTO dto) {
        return ResponseEntity.ok(categorieService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une catégorie (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feuilles")
    @Operation(summary = "Liste toutes les catégories feuilles (sans enfant)")
    public ResponseEntity<List<CategorieResponseDTO>> getFeuilles() {
        return ResponseEntity.ok(categorieService.findFeuilles());
    }

    @GetMapping("/{id}/feuilles")
    @Operation(summary = "Feuilles descendantes d'une catégorie")
    public ResponseEntity<List<CategorieResponseDTO>> getFeuillesDescendants(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findFeuillesDescendants(id));
    }
}