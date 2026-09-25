package com.innovatech.api.controller;

import com.innovatech.api.dto.ProduitRequestDTO;
import com.innovatech.api.dto.ProduitResponseDTO;
import com.innovatech.api.service.ProduitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produits")
@RequiredArgsConstructor
public class ProduitController {

    private final ProduitService produitService;

    @GetMapping
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

    @GetMapping("/categorie/{categorie}")
    public ResponseEntity<List<ProduitResponseDTO>> getByCategorie(@PathVariable String categorie) {
        return ResponseEntity.ok(produitService.findByCategorie(categorie));
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        produitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        produitService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}