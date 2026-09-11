package com.ecole.gestion_scolaire.finance.dto.fee;

public record FeeTypeResponse(Long id, String code, String name, String category, boolean active,
                              Integer displayOrder) {
}
