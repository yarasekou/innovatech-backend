package com.innovatech.api.exception;

/**
 * Exception lancée quand une règle métier est violée.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}