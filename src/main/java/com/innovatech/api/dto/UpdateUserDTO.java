package com.innovatech.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Modification d'un USER par son ADMIN.
 * <p>Ni l'email ni le username ne sont modifiables (identifiants uniques).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserDTO {

    @Size(max = 100)
    private String prenom;

    @Size(max = 100)
    private String nom;

    @Size(min = 6, max = 100, message = "Le mot de passe doit contenir entre 6 et 100 caractères")
    private String password;   // optionnel
}