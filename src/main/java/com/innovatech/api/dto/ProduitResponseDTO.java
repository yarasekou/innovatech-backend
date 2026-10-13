package com.innovatech.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    // Entreprise propriétaire
    private Long entrepriseId;
    private String entrepriseNom;

    // Catégorie
    private Long categorieId;
    private String categorieNom;
    private String categorieSlug;
    private String categorieChemin;
    private Integer categorieNiveau;

    // User créateur (traçabilité)
    private Long userId;
    private String userEmail;
    private String userNom;
    private String userPrenom;

    // Photos
    private List<PhotoProduitDTO> photos;
    private String photoPrincipale;
    private Integer nbPhotos;

    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}