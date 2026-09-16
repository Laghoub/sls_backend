package com.ecole.gestion_scolaire.payroll.dto; import java.time.*;
public record PayrollPeriodResponse(Long id,Long schoolYearId,String schoolYear,int year,int month,LocalDate startDate,LocalDate endDate,String status){}
