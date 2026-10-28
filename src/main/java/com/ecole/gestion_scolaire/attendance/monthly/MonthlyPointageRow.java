package com.ecole.gestion_scolaire.attendance.monthly;
import java.math.BigDecimal;
import java.time.LocalDate;
public record MonthlyPointageRow(Long teacherId, String teacherName, Long classId, String className,
                                 Long subjectId, String subjectName, LocalDate date, BigDecimal plannedHours,
                                 BigDecimal performedHours, String status, Long sessionId) {}
