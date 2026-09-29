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

    @NotNull(message = "Le prix de vente est obligatoire")
    @DecimalMin(value = "0.00", message = "Le prix de vente doit être positif ou nul")
    @Digits(integer = 10, fraction = 2, message = "Format du prix de vente invalide")
    private BigDecimal prix;

    @NotNull(message = "Le prix d'achat est obligatoire")
    @DecimalMin(value = "0.00", message = "Le prix d'achat doit être positif ou nul")
    @Digits(integer = 10, fraction = 2, message = "Format du prix d'achat invalide")
    private BigDecimal prixAchat;

    @NotNull(message = "Le stock est obligatoire")
    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    private Integer stock;

    /**
     * ID de la catégorie (optionnel).
     */
    private Long categorieId;

    @Builder.Default
    private Boolean actif = true;
}