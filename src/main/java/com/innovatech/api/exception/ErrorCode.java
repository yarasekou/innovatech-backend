package com.innovatech.api.exception;

/**
 * Codes d'erreur applicatifs, utiles côté frontend
 * pour identifier le type d'erreur sans parser le message.
 */
public enum ErrorCode {

    RESOURCE_NOT_FOUND("Ressource introuvable"),
    BUSINESS_ERROR("Erreur métier"),
    VALIDATION_ERROR("Erreur de validation"),
    UNAUTHORIZED("Non autorisé"),
    FORBIDDEN("Accès refusé"),
    INTERNAL_ERROR("Erreur interne du serveur");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}