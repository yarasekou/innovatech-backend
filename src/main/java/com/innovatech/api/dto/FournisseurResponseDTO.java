package com.innovatech.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FournisseurResponseDTO {

    private Long id;
    private String code;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
    private String ville;
    private String pays;
    private String siret;
    private String contactNom;
    private boolean actif;

    // Entreprise propriétaire
    private Long entrepriseId;
    private String entrepriseNom;

    // User créateur (traçabilité)
    private Long userId;
    private String userEmail;
    private String userNom;
    private String userPrenom;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}