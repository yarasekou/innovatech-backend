package com.innovatech.api.controller;

import com.innovatech.api.dto.ClientRequestDTO;
import com.innovatech.api.dto.ClientResponseDTO;
import com.innovatech.api.service.ClientService;
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
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Clients scopés par boutique (USER)")
public class ClientController {

    private final ClientService clientService;

    // -----------------------------------------
    // READ — ADMIN, USER, SELLER
    // -----------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Liste les clients accessibles à l'utilisateur connecté")
    public ResponseEntity<List<ClientResponseDTO>> getAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/actifs")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Liste les clients actifs accessibles")
    public ResponseEntity<List<ClientResponseDTO>> getActifs() {
        return ResponseEntity.ok(clientService.findAllActifs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Détail d'un client")
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
    @Operation(summary = "Rechercher des clients")
    public ResponseEntity<List<ClientResponseDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(clientService.search(query));
    }

    // -----------------------------------------
    // CREATE — USER uniquement (SELLER aussi ?)
    // ⚠️ ADMIN ne crée PAS de client
    // -----------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Créer un client dans ma boutique")
    public ResponseEntity<ClientResponseDTO> create(
            @Valid @RequestBody ClientRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        ClientResponseDTO created = clientService.create(dto, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE — USER uniquement
    // -----------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Modifier un client de ma boutique")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequestDTO dto) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }

    // -----------------------------------------
    // DELETE / DEACTIVATE — USER uniquement
    // -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Supprimer un client de ma boutique")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Désactiver un client de ma boutique")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        clientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}