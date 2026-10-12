package com.innovatech.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseRequestDTO {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères")
    private String nom;

    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;

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

    @Size(max = 10, message = "La devise ne doit pas dépasser 10 caractères")
    @Builder.Default
    private String devise = "XOF";

    @DecimalMin(value = "0.00", message = "Le taux de TVA doit être positif ou nul")
    @Digits(integer = 3, fraction = 2, message = "TVA : max 3 chiffres entiers et 2 décimales")
    @Builder.Default
    private BigDecimal tvaPercent = new BigDecimal("18.00");
}