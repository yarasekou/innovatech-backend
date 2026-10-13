package com.innovatech.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Objects;

/**
 * Utilitaires pour accéder au contexte de sécurité.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Classe utilitaire
    }

    /**
     * Retourne l'email du user connecté (ou null si non authentifié).
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return null;
    }

    /**
     * Vérifie si l'utilisateur connecté a un rôle donné.
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), role));
    }

    // -----------------------------------------
    // Rôles
    // -----------------------------------------

    /**
     * SUPER_ADMIN (plateforme InnovaTech).
     */
    public static boolean isSuperAdmin() {
        return hasRole("ROLE_SUPER_ADMIN");
    }

    /**
     * ADMIN (owner d'une entreprise).
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * USER (manager interne).
     */
    public static boolean isUser() {
        return hasRole("ROLE_USER");
    }

    /**
     * SELLER (vendeur terrain).
     */
    public static boolean isSeller() {
        return hasRole("ROLE_SELLER");
    }

    // -----------------------------------------
    // Combinaisons
    // -----------------------------------------

    /**
     * True si l'utilisateur est un tenant (ADMIN, USER ou SELLER).
     * C'est-à-dire qu'il appartient à une entreprise.
     */
    public static boolean isTenant() {
        return isAdmin() || isUser() || isSeller();
    }

    /**
     * True si ADMIN ou SELLER (utilisé pour certains endpoints métier).
     * @deprecated Utiliser isTenant() à la place.
     */
    @Deprecated
    public static boolean isAdminOrSeller() {
        return isAdmin() || isSeller();
    }
}