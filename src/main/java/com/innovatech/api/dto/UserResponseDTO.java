package com.innovatech.api.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO de sortie pour un utilisateur (jamais de password).
 */
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
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}