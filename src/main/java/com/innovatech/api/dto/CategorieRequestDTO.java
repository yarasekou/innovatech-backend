package com.innovatech.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères")
    private String nom;

    /**
     * Slug optionnel — s'il est absent, il sera généré automatiquement depuis le nom.
     */
    @Size(max = 150, message = "Le slug ne doit pas dépasser 150 caractères")
    @Pattern(
            regexp = "^[a-z0-9-]*$",
            message = "Le slug ne doit contenir que des minuscules, chiffres et tirets"
    )
    private String slug;

    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    /**
     * ID du parent (null si catégorie racine).
     */
    private Long parentId;

    @Builder.Default
    private Boolean actif = true;
}