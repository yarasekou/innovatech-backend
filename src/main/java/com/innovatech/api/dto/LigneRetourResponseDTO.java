package com.innovatech.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneRetourResponseDTO {

    private Long id;
    private Long produitId;
    private String produitReference;
    private String produitNom;
    private Integer quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal sousTotal;
    private String raison;
}