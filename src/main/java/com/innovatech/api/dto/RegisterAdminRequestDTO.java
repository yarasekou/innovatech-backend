package com.innovatech.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Inscription d'un ADMIN + création de son entreprise en une seule transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterAdminRequestDTO {

    // -----------------------------------------
    // Infos du compte ADMIN
    // -----------------------------------------

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, max = 100, message = "Le mot de passe doit contenir entre 6 et 100 caractères")
    private String password;

    @Size(max = 100)
    private String prenom;

    @Size(max = 100)
    private String nom;

    // -----------------------------------------
    // Infos de l'entreprise
    // -----------------------------------------

    @NotNull(message = "Les informations de l'entreprise sont obligatoires")
    @Valid
    private EntrepriseRequestDTO entreprise;
}