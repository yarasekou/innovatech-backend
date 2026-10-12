package com.innovatech.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseResponseDTO {

    private Long id;
    private String nom;
    private String slug;
    private String description;
    private String email;
    private String telephone;
    private String adresse;
    private String ville;
    private String pays;
    private String siret;
    private String devise;
    private BigDecimal tvaPercent;
    private String logo;          // nom du fichier
    private String logoUrl;       // URL complète
    private boolean actif;

    // Propriétaire
    private Long userId;
    private String userEmail;
    private String userNom;
    private String userPrenom;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}