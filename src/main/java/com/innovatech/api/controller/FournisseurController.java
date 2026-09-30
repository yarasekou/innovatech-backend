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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fournisseurs")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs", description = "Fournisseurs auprès desquels InnovaTech achète")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    @GetMapping
    @Operation(summary = "Liste tous les fournisseurs")
    public ResponseEntity<List<FournisseurResponseDTO>> getAll() {
        return ResponseEntity.ok(fournisseurService.findAll());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<FournisseurResponseDTO>> getActifs() {
        return ResponseEntity.ok(fournisseurService.findAllActifs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FournisseurResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fournisseurService.findById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<FournisseurResponseDTO> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(fournisseurService.findByCode(code));
    }

    @GetMapping("/pays/{pays}")
    public ResponseEntity<List<FournisseurResponseDTO>> getByPays(@PathVariable String pays) {
        return ResponseEntity.ok(fournisseurService.findByPays(pays));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FournisseurResponseDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(fournisseurService.search(query));
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau fournisseur")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FournisseurResponseDTO> create(
            @Valid @RequestBody FournisseurRequestDTO dto) {
        FournisseurResponseDTO created = fournisseurService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FournisseurResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FournisseurRequestDTO dto) {
        return ResponseEntity.ok(fournisseurService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un fournisseur (ADMIN uniquement)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fournisseurService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        fournisseurService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}