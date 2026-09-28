package com.innovatech.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FournisseurRequestDTO {

    @NotBlank(message = "Le code est obligatoire")
    @Size(min = 2, max = 50, message = "Le code doit contenir entre 2 et 50 caractères")
    private String code;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères")
    private String nom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
    private String email;

    @Pattern(
            regexp = "^[0-9+\\-\\s()]*$",
            message = "Le téléphone ne doit contenir que des chiffres, espaces, +, -, (, )"
    )
    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    private String telephone;

    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String adresse;

    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
    private String ville;

    @Size(max = 100, message = "Le pays ne doit pas dépasser 100 caractères")
    private String pays;

    @Pattern(
            regexp = "^[0-9]{14}$",
            message = "Le SIRET doit contenir exactement 14 chiffres"
    )
    private String siret;

    @Size(max = 150, message = "Le nom du contact ne doit pas dépasser 150 caractères")
    private String contactNom;

    @Builder.Default
    private Boolean actif = true;
}