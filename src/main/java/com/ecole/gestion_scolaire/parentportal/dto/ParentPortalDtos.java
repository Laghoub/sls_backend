package com.ecole.gestion_scolaire.parentportal.dto;
import java.math.BigDecimal; import java.time.*; import java.util.*;
public final class ParentPortalDtos {private ParentPortalDtos(){}
 public record Me(Long guardianId,String matricule,String firstName,String lastName,String email){}
 public record Child(Long studentId,String studentNumber,String firstName,String lastName,Long enrollmentId,Long classGroupId,String className,boolean financialResponsible){}
 public record Dashboard(int children,BigDecimal totalDue,BigDecimal totalPaid,BigDecimal remaining,long overdueCharges){}
 public record FinanceSummary(Long studentId,BigDecimal totalDue,BigDecimal totalPaid,BigDecimal remaining,long overdueCharges){}
 public record Charge(Long id,String label,BigDecimal originalAmount,BigDecimal discountAmount,BigDecimal finalAmount,BigDecimal paidAmount,BigDecimal remaining,LocalDate dueDate,String status){}
 public record PaymentItem(Long id,String paymentNumber,OffsetDateTime paymentDate,BigDecimal totalAmount,String status,String paymentMethod,String externalReference){}
 public record AttendanceItem(Long id,LocalDate date,String subject,String status,Integer lateMinutes,String reason,boolean justified){}
 public record TimetableItem(Long id,String dayOfWeek,String startTime,String endTime,String subject,String teacher,String className){}
 public record ExerciseItem(Long id,String title,String description,String instructions,LocalDate dueDate,String status,String subject,String className){}
 public record GradeItem(Long evaluationId,String evaluation,String subject,LocalDate evaluationDate,BigDecimal maxScore,BigDecimal score,boolean absent,String comment){}
}
