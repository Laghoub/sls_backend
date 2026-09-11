package com.ecole.gestion_scolaire.finance.dto.refund;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RefundRequest(@NotNull Long paymentId, @NotNull @DecimalMin("0.01") BigDecimal amount,
                            @NotNull Long paymentMethodId, @NotBlank String reason, Long cashRegisterSessionId) {
}
