package com.innovatech.api.entity;

/**
 * Rôles applicatifs.
 * <p>
 * - ROLE_USER   : peut voir ses propres données
 * - ROLE_SELLER : ROLE_USER + peut créer/modifier ses produits et gérer ses commandes
 * - ROLE_ADMIN  : peut tout voir et tout modifier
 */
public enum Role {
    ROLE_USER,
    ROLE_SELLER,
    ROLE_ADMIN
}