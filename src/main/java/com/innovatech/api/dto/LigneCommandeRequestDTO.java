package com.innovatech.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO d'entrée pour une ligne de commande.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneCommandeRequestDTO {

    @NotNull(message = "L'identifiant du produit est obligatoire")
    private Long produitId;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;
}