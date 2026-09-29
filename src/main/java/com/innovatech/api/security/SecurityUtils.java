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
     * Retourne true si l'utilisateur connecté a le rôle ROLE_ADMIN.
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Retourne true si l'utilisateur connecté a le rôle ROLE_SELLER.
     */
    public static boolean isSeller() {
        return hasRole("ROLE_SELLER");
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

    /**
     * Retourne true si l'utilisateur est admin OU seller.
     */
    public static boolean isAdminOrSeller() {
        return isAdmin() || isSeller();
    }
}