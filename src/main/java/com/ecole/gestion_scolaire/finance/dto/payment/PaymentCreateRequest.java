package com.ecole.gestion_scolaire.finance.dto.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PaymentCreateRequest(@NotNull Long guardianId, OffsetDateTime paymentDate, @NotNull Long paymentMethodId,
                                   @NotNull @DecimalMin("0.01") BigDecimal totalAmount, Long cashRegisterSessionId,
                                   String externalReference, String notes,
                                   @NotNull @Valid List<PaymentAllocationRequest> allocations) {
}
