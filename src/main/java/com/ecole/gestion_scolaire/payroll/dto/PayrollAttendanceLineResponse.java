package com.ecole.gestion_scolaire.payroll.dto; import java.math.BigDecimal; import java.time.LocalDate;
public record PayrollAttendanceLineResponse(Long attendanceId,LocalDate date,String className,String subjectName,String timeRange,String attendanceStatus,Integer lateMinutes,Integer scheduledMinutes,Integer paidMinutes,BigDecimal appliedHourlyRate,BigDecimal amount){}
