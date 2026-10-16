package com.innovatech.api.controller;

import com.innovatech.api.dto.StockFournisseurDTO;
import com.innovatech.api.service.StockFournisseurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks-fournisseur")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER')")
@Tag(name = "Stocks fournisseur", description = "Stock des produits par fournisseur")
public class StockFournisseurController {

    private final StockFournisseurService stockService;

    @GetMapping
    @Operation(summary = "Liste les stocks fournisseur accessibles")
    public ResponseEntity<List<StockFournisseurDTO>> getAll() {
        return ResponseEntity.ok(stockService.findAll());
    }

    @GetMapping("/produit/{produitId}")
    @Operation(summary = "Stocks d'un produit (tous fournisseurs)")
    public ResponseEntity<List<StockFournisseurDTO>> getByProduit(@PathVariable Long produitId) {
        return ResponseEntity.ok(stockService.findByProduit(produitId));
    }
}