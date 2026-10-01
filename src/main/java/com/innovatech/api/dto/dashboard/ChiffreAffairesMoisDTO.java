package com.innovatech.api.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * Chiffre d'affaires par mois.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiffreAffairesMoisDTO {

    private Integer annee;
    private Integer mois;
    private String libelle;        // "Octobre 2026"
    private BigDecimal chiffreAffaires;
    private Long nbCommandes;
}