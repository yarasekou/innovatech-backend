package com.innovatech.api.entity;

/**
 * Méthodes de paiement acceptées.
 */
public enum MethodePaiement {

    ESPECES("Espèces"),
    VIREMENT("Virement bancaire"),
    CARTE("Carte bancaire"),
    MOBILE_MONEY("Mobile Money"),
    CHEQUE("Chèque"),
    WAVE("Wave"),
    ORANGE_MONEY("Orange Money");

    private final String libelle;

    MethodePaiement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}