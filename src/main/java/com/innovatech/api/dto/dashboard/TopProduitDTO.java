package com.innovatech.api.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * Top produit vendu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProduitDTO {

    private Long produitId;
    private String reference;
    private String nom;
    private Long quantiteVendue;
    private BigDecimal chiffreAffaires;
}