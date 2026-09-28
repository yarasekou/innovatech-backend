package com.innovatech.api.entity;

/**
 * Statuts possibles d'un paiement.
 */
public enum StatutPaiement {

    EN_ATTENTE("En attente de validation"),
    VALIDE("Validé"),
    REMBOURSE("Remboursé"),
    ANNULE("Annulé");

    private final String libelle;

    StatutPaiement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}