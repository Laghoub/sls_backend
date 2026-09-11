package com.ecole.gestion_scolaire.finance.exception;

public class FinanceNotFoundException extends RuntimeException {
    public FinanceNotFoundException(String message) {
        super(message);
    }
}
