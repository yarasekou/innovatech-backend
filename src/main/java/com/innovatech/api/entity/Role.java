package com.innovatech.api.entity;

/**
 * Rôles applicatifs.
 * <p>
 * Hiérarchie :
 * <ul>
 *   <li>ROLE_SUPER_ADMIN : équipe InnovaTech, voit TOUT</li>
 *   <li>ROLE_ADMIN       : owner d'UNE entreprise, voit TOUT dans son entreprise</li>
 *   <li>ROLE_USER        : manager interne, créé par un ADMIN</li>
 *   <li>ROLE_SELLER      : vendeur terrain, créé par un USER</li>
 * </ul>
 */
public enum Role {
    ROLE_SUPER_ADMIN,
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_SELLER
}