package com.ecole.gestion_scolaire.termevaluation;
import jakarta.validation.constraints.*;
import java.time.*;
public record CampaignRequest(@NotNull Long schoolYearId,@NotBlank String title,
 @NotNull @Min(1) @Max(3) Integer trimester,String cycleCode,
 @NotNull LocalDate evaluationFrom,@NotNull LocalDate evaluationTo,
 @NotNull OffsetDateTime opensAt,@NotNull OffsetDateTime closesAt) {}
