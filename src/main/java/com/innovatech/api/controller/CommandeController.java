package com.innovatech.api.controller;

import com.innovatech.api.dto.CommandeRequestDTO;
import com.innovatech.api.dto.CommandeResponseDTO;
import com.innovatech.api.entity.StatutCommande;
import com.innovatech.api.service.CommandeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/commandes")
@RequiredArgsConstructor
public class CommandeController {

    private final CommandeService commandeService;

    @GetMapping
    public ResponseEntity<List<CommandeResponseDTO>> getAll() {
        return ResponseEntity.ok(commandeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.findById(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<CommandeResponseDTO> getByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(commandeService.findByNumero(numero));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<CommandeResponseDTO>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(commandeService.findByClient(clientId));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<CommandeResponseDTO>> getByStatut(@PathVariable StatutCommande statut) {
        return ResponseEntity.ok(commandeService.findByStatut(statut));
    }

    @PostMapping
    public ResponseEntity<CommandeResponseDTO> create(
            @Valid @RequestBody CommandeRequestDTO dto) {
        CommandeResponseDTO created = commandeService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommandeResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CommandeRequestDTO dto) {
        return ResponseEntity.ok(commandeService.update(id, dto));
    }

    @PatchMapping("/{id}/statut/{nouveauStatut}")
    public ResponseEntity<CommandeResponseDTO> changerStatut(
            @PathVariable Long id,
            @PathVariable StatutCommande nouveauStatut) {
        return ResponseEntity.ok(commandeService.changerStatut(id, nouveauStatut));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}