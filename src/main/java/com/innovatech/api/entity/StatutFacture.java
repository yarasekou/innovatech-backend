package com.innovatech.api.entity;

/**
 * Statuts d'une facture.
 * <p>
 * Cycle de vie normal :
 * BROUILLON → EMISE → PAYEE
 * <p>
 * Cas particuliers :
 * - EN_RETARD : calculé (dateEcheance dépassée, non payée)
 * - ANNULEE   : annulation manuelle
 */
public enum StatutFacture {

    BROUILLON("Brouillon (non émise)"),
    EMISE("Émise (en attente de paiement)"),
    PAYEE("Payée"),
    EN_RETARD("En retard de paiement"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutFacture(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}