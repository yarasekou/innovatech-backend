package com.innovatech.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitRequestDTO {

    @NotBlank(message = "La référence est obligatoire")
    @Size(min = 2, max = 50, message = "La référence doit contenir entre 2 et 50 caractères")
    private String reference;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères")
    private String nom;

    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.00", message = "Le prix doit être positif ou nul")
    @Digits(integer = 10, fraction = 2, message = "Le prix doit avoir au maximum 10 chiffres entiers et 2 décimales")
    private BigDecimal prix;

    @NotNull(message = "Le stock est obligatoire")
    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    private Integer stock;

    @Size(max = 100, message = "La catégorie ne doit pas dépasser 100 caractères")
    private String categorie;

    @Builder.Default
    private Boolean actif = true;
}