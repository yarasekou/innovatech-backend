package com.innovatech.api.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Récapitulatif du solde d'une commande.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResteAPayerDTO {

    private Long commandeId;
    private String commandeNumero;
    private BigDecimal totalTTC;
    private BigDecimal totalPaye;
    private BigDecimal resteAPayer;
    private boolean solde;
}