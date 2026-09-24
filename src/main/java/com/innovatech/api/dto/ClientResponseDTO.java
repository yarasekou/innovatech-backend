package com.innovatech.api.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de sortie pour un client.
 * <p>
 * Représente ce que l'API renvoie au client HTTP.
 */
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
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}