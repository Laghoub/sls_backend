package com.ecole.gestion_scolaire.finance.dto.discount;
import jakarta.validation.constraints.NotBlank;
public record DiscountRejectRequest(@NotBlank String reason){}
