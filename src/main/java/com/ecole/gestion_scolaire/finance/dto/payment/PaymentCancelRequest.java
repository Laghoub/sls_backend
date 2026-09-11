package com.ecole.gestion_scolaire.finance.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record PaymentCancelRequest(@NotBlank String reason) {
}
