package com.innovatech.api.controller;

import com.innovatech.api.dto.PhotoProduitDTO;
import com.innovatech.api.service.PhotoProduitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/produits/{produitId}/photos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")   // ← au niveau classe
@Tag(name = "Photos de produit", description = "Gestion des photos de produits")
public class PhotoProduitController {

    private final PhotoProduitService photoService;

    // -----------------------------------------
    // READ — ADMIN peut aussi consulter (utile pour supervision interne)
    // -----------------------------------------

    @GetMapping
    @Operation(summary = "Liste les photos d'un produit")
    public ResponseEntity<List<PhotoProduitDTO>> getPhotos(@PathVariable Long produitId) {
        return ResponseEntity.ok(photoService.findByProduit(produitId));
    }

    // -----------------------------------------
    // CREATE — USER / SELLER (le service filtre déjà par boutique)
    // -----------------------------------------

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Ajouter une photo à un produit")
    public ResponseEntity<PhotoProduitDTO> ajouter(
            @PathVariable Long produitId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "titre", required = false) String titre,
            @RequestParam(value = "principale", defaultValue = "false") boolean principale) {

        PhotoProduitDTO created = photoService.ajouter(produitId, file, titre, principale);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------
    // UPDATE — USER / SELLER
    // -----------------------------------------

    @PatchMapping("/{photoId}/principale")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Définir une photo comme principale")
    public ResponseEntity<PhotoProduitDTO> setPrincipale(@PathVariable Long produitId,
                                                         @PathVariable Long photoId) {
        return ResponseEntity.ok(photoService.setPrincipale(photoId));
    }

    // -----------------------------------------
    // DELETE — USER / SELLER
    // -----------------------------------------

    @DeleteMapping("/{photoId}")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    @Operation(summary = "Supprimer une photo")
    public ResponseEntity<Void> supprimer(@PathVariable Long produitId,
                                          @PathVariable Long photoId) {
        photoService.supprimer(photoId);
        return ResponseEntity.noContent().build();
    }
}