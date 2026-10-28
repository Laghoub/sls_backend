package com.ecole.gestion_scolaire.attendance.monthly;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
public record MonthlyPointageSummary(Long teacherId, String teacherName, Long classId,
 String className, Map<LocalDate,BigDecimal> hoursByDay, BigDecimal totalHours) {}
