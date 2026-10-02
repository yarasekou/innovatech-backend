package com.innovatech.api.entity;

/**
 * Statuts possibles d'un retour client.
 */
public enum StatutRetour {

    DEMANDE("Demande de retour"),
    ACCEPTE("Accepté"),
    REFUSE("Refusé"),
    RECU("Reçu (stock restitué)"),
    REMBOURSE("Remboursé");

    private final String libelle;

    StatutRetour(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}