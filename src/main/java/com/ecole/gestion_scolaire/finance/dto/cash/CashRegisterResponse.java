package com.ecole.gestion_scolaire.finance.dto.cash;

public record CashRegisterResponse(Long id, String code, String name, Long campusId, boolean active) {
}
