package com.ecole.gestion_scolaire.teacherportal.dto;
import java.math.BigDecimal; import java.time.*; import java.util.*;
public final class TeacherPortalDtos { private TeacherPortalDtos(){}
 public record Me(Long teacherId,Long employeeId,String employeeNumber,String firstName,String lastName,String email){}
 public record Dashboard(Me teacher,long classCount,long studentCount,long presentCount,long absentCount,long lateCount,BigDecimal latestNetSalary,BigDecimal latestRemainingSalary){}
 public record Assignment(Long assignmentId,Long schoolYearId,Long classGroupId,String classGroupName,Long subjectId,String subjectName,LocalDate startDate,LocalDate endDate,String status){}
 public record StudentItem(Long studentId,String studentNumber,String firstName,String lastName,String status){}
 public record Attendance(Long id,LocalDate date,String className,String subjectName,String timeSlot,String status,Integer lateMinutes,String reason,String validationStatus){}
 public record PayrollItem(Long id,int year,int month,String compensationType,BigDecimal grossSalary,BigDecimal netSalary,BigDecimal paidAmount,BigDecimal remainingAmount,String status){}
 public record ExerciseRequest(Long teachingAssignmentId,String title,String description,String instructions,LocalDate dueDate,String attachmentReference,String status){}
 public record Exercise(Long id,Long teachingAssignmentId,Long classGroupId,String classGroupName,Long subjectId,String subjectName,String title,String description,String instructions,LocalDate dueDate,String attachmentReference,String status){}
 public record EvaluationRequest(Long teachingAssignmentId,String title,LocalDate evaluationDate,BigDecimal maxScore,BigDecimal coefficient,String description,String status){}
 public record Evaluation(Long id,Long teachingAssignmentId,Long classGroupId,String classGroupName,Long subjectId,String subjectName,String title,LocalDate evaluationDate,BigDecimal maxScore,BigDecimal coefficient,String description,String status){}
 public record GradeRequest(Long studentId,BigDecimal score,boolean absent,String comment){}
 public record Grade(Long id,Long studentId,String studentNumber,String firstName,String lastName,BigDecimal score,boolean absent,String comment){}
}
