package com.innovatech.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponseDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;
    private String ville;
    private String pays;
    private String siret;
    private boolean actif;

    // Propriétaire
    private Long userId;
    private String userEmail;
    private String userNom;
    private String userPrenom;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}