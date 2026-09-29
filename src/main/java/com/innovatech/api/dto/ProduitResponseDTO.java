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

    private BigDecimal prix;
    private BigDecimal prixAchat;
    private BigDecimal margeUnitaire;
    private BigDecimal tauxMarge;

    private Integer stock;

    // -----------------------------------------
    // Catégorie (version aplatie)
    // -----------------------------------------
    private Long categorieId;
    private String categorieNom;
    private String categorieSlug;
    private String categorieChemin;      // "Accessoire > Téléphone > Google Pixel"
    private Integer categorieNiveau;

    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}