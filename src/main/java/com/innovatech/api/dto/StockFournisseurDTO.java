package com.innovatech.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockFournisseurDTO {

    private Long id;
    private Long produitId;
    private String produitReference;
    private String produitNom;
    private Long fournisseurId;
    private String fournisseurCode;
    private String fournisseurNom;
    private Integer stock;
    private BigDecimal prixAchat;
    private LocalDateTime dateModification;
}