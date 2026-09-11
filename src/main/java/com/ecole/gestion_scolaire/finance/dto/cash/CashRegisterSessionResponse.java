package com.ecole.gestion_scolaire.finance.dto.cash;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CashRegisterSessionResponse(Long id, Long cashRegisterId, Long cashierId, OffsetDateTime openedAt,
                                          BigDecimal openingBalance, OffsetDateTime closedAt,
                                          BigDecimal expectedClosingBalance, BigDecimal actualClosingBalance,
                                          BigDecimal differenceAmount, String status, Long closedBy) {
}
