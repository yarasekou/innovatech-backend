package com.innovatech.api.exception;

/**
 * Exception lancée quand une ressource demandée n'existe pas.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s introuvable avec l'id : %d", resource, id));
    }
}