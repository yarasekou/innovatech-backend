package com.innovatech.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneCommandeAchatRequestDTO {

    @NotNull(message = "L'identifiant du produit est obligatoire")
    private Long produitId;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    /**
     * Prix d'achat négocié (optionnel).
     * Si null, on utilise le prixAchat du produit.
     */
    @DecimalMin(value = "0.00", message = "Le prix d'achat doit être positif ou nul")
    @Digits(integer = 10, fraction = 2, message = "Prix d'achat invalide")
    private BigDecimal prixAchatUnitaire;
}