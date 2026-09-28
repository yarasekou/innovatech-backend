package com.innovatech.api.entity;

/**
 * Statuts d'une commande d'achat.
 */
public enum StatutAchat {

    EN_ATTENTE("En attente"),
    COMMANDEE("Commandée au fournisseur"),
    RECU_PARTIEL("Reçu partiellement"),
    RECUE("Reçue"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutAchat(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}