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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")   // ← SUPER_ADMIN exclu
@Tag(name = "Catégories", description = "Catégories scopées par entreprise")
public class CategorieController {

    private final CategorieService categorieService;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    @GetMapping
    public ResponseEntity<List<CategorieResponseDTO>> getAll() {
        return ResponseEntity.ok(categorieService.findAll());
    }

    @GetMapping("/racines")
    public ResponseEntity<List<CategorieResponseDTO>> getRacines() {
        return ResponseEntity.ok(categorieService.findRacines());
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategorieTreeDTO>> getTree() {
        return ResponseEntity.ok(categorieService.getTree());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorieResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findById(id));
    }

    @GetMapping("/{id}/tree")
    public ResponseEntity<CategorieTreeDTO> getTreeFrom(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.getTreeFrom(id));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<CategorieResponseDTO>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findByParent(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategorieResponseDTO> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categorieService.findBySlug(slug));
    }

    @GetMapping("/feuilles")
    public ResponseEntity<List<CategorieResponseDTO>> getFeuilles() {
        return ResponseEntity.ok(categorieService.findFeuilles());
    }

    @GetMapping("/{id}/feuilles")
    public ResponseEntity<List<CategorieResponseDTO>> getFeuillesDescendants(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findFeuillesDescendants(id));
    }

    // -----------------------------------------
    // CREATE — USER / SELLER
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Créer une catégorie dans ma boutique")
    public ResponseEntity<CategorieResponseDTO> create(
            @Valid @RequestBody CategorieRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        CategorieResponseDTO created =
                categorieService.create(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE — USER / SELLER
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<CategorieResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CategorieRequestDTO dto) {
        return ResponseEntity.ok(categorieService.update(id, dto));
    }

    // -----------------------------------------
    // DELETE — USER / SELLER
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}