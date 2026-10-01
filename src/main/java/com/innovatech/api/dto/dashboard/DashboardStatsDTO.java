package com.innovatech.api.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * Statistiques globales du dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // Chiffre d'affaires
    private BigDecimal chiffreAffairesHT;
    private BigDecimal chiffreAffairesTTC;
    private BigDecimal margeTotale;

    // Compteurs
    private Long nbClients;
    private Long nbProduits;
    private Long nbCommandes;
    private Long nbPaiements;
    private Long nbCommandesAchat;

    // Commandes par statut
    private Long nbCommandesEnAttente;
    private Long nbCommandesValidees;
    private Long nbCommandesEnCours;
    private Long nbCommandesLivrees;
    private Long nbCommandesAnnulees;

    // Moyennes
    private BigDecimal panierMoyen;
}