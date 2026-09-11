package com.ecole.gestion_scolaire.finance.dto.tariff;

import java.math.BigDecimal;
import java.time.*;

public record TariffResponse(Long id, Long schoolYearId, Long feeTypeId, Long levelId, Long classGroupId, Long campusId,
                             BigDecimal amount, String billingFrequency, LocalDate validFrom, LocalDate validUntil,
                             boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
