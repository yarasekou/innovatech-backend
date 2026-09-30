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
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Gestion des clients d'InnovaTech")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @Operation(summary = "Liste tous les clients")
    public ResponseEntity<List<ClientResponseDTO>> getAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<ClientResponseDTO>> getActifs() {
        return ResponseEntity.ok(clientService.findAllActifs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ClientResponseDTO>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(clientService.search(query));
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau client")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ClientResponseDTO> create(
            @Valid @RequestBody ClientRequestDTO dto) {
        ClientResponseDTO created = clientService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequestDTO dto) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un client (ADMIN uniquement)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        clientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }


}