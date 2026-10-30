package com.ecole.gestion_scolaire.termevaluation;
import jakarta.validation.constraints.*;
import java.util.*;
public record TeacherEvaluationRequest(
 @NotNull Long campaignId,@NotNull Long studentId,@NotNull Long classGroupId,@NotNull Long subjectId,
 String observation,String difficulties,String recommendations,
 List<Answer> answers) {
 public record Answer(@NotNull Long criterionId,Long optionId,String freeText){}
}
