package com.ecole.gestion_scolaire.payroll.dto; import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record SalaryPaymentRequest(@NotNull @DecimalMin(value="0.01") BigDecimal amount,@NotNull Long paymentMethodId,Long cashRegisterSessionId,String reference){}
