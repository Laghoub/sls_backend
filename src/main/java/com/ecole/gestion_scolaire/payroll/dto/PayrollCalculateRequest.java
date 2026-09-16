package com.ecole.gestion_scolaire.payroll.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PayrollCalculateRequest(@NotNull Long teacherId,@NotNull Long schoolYearId,@Min(2000) int year,@Min(1) @Max(12) int month,@PositiveOrZero BigDecimal absenceDeduction){}
