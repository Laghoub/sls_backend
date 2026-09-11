package com.ecole.gestion_scolaire.finance.dto.cash;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CashSessionCloseRequest(@NotNull @DecimalMin("0.00") BigDecimal actualClosingBalance) {
}
