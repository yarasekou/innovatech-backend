package com.innovatech.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitResponseDTO {

    private Long id;
    private String reference;
    private String nom;
    private String description;

    // Prix
    private BigDecimal prix;           // Prix de vente
    private BigDecimal prixAchat;      // Prix d'achat

    // Marge (calculée)
    private BigDecimal margeUnitaire;
    private BigDecimal tauxMarge;

    private Integer stock;
    private String categorie;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}