package com.innovatech.api.controller;

import com.innovatech.api.dto.RetourRequestDTO;
import com.innovatech.api.dto.RetourResponseDTO;
import com.innovatech.api.entity.StatutRetour;
import com.innovatech.api.service.RetourService;
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
@RequestMapping("/retours")
@RequiredArgsConstructor
@Tag(name = "Retours clients", description = "Retours scopés par boutique")
public class RetourController {

    private final RetourService retourService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<RetourResponseDTO>> findAll() {
        return ResponseEntity.ok(retourService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<RetourResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(retourService.findById(id));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<RetourResponseDTO>> findByStatut(@PathVariable StatutRetour statut) {
        return ResponseEntity.ok(retourService.findByStatut(statut));
    }

    @GetMapping("/commande/{commandeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    public ResponseEntity<List<RetourResponseDTO>> findByCommande(@PathVariable Long commandeId) {
        return ResponseEntity.ok(retourService.findByCommande(commandeId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Créer une demande de retour")
    public ResponseEntity<RetourResponseDTO> create(
            @Valid @RequestBody RetourRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        RetourResponseDTO created = retourService.create(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Changer le statut d'un retour")
    public ResponseEntity<RetourResponseDTO> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutRetour statut) {
        return ResponseEntity.ok(retourService.changerStatut(id, statut));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        retourService.delete(id);
        return ResponseEntity.noContent().build();
    }
}