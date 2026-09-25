package com.innovatech.api.entity;

import lombok.Getter;

/**
 * Statuts possibles d'une commande.
 */
@Getter
public enum StatutCommande {

    EN_ATTENTE("En attente de validation"),
    VALIDEE("Validée, à traiter"),
    EN_COURS("En cours de traitement"),
    LIVREE("Livrée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutCommande(String libelle) {
        this.libelle = libelle;
    }

}