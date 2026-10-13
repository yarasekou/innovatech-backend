package com.innovatech.api.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String prenom;
    private String nom;
    private Set<String> roles;
    private boolean actif;

    // Entreprise d'appartenance
    private Long entrepriseId;
    private String entrepriseNom;

    // Relation hiérarchique : SELLER → USER (patron)
    private Long patronId;
    private String patronEmail;
    private String patronNom;
    private String patronPrenom;

    // Nombre d'employés (utile pour l'UI)
    private Integer nbEmployes;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}