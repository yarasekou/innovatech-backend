package com.innovatech.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneCommandeAchatResponseDTO {

    private Long id;
    private Long produitId;
    private String produitReference;
    private String produitNom;
    private Integer quantite;
    private BigDecimal prixAchatUnitaire;
    private BigDecimal sousTotal;
}