package com.innovatech.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieResponseDTO {

    private Long id;
    private String nom;
    private String slug;
    private String description;
    private Long parentId;
    private String parentNom;
    private Integer niveau;
    private String cheminComplet;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Entreprise propriétaire
    private Long entrepriseId;
    private String entrepriseNom;

    // User propriétaire de la boutique
    private Long userId;
    private String userEmail;
}