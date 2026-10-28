package com.ecole.gestion_scolaire.transport.dto;
import java.time.*; import java.math.*; import jakarta.validation.constraints.*;
public final class TransportDtos { private TransportDtos(){}
 public record VehicleRequest(@NotBlank String code,@NotBlank String registrationNumber,String brand,String model,@NotNull @Min(1) Integer capacity,LocalDate insuranceExpiry,LocalDate technicalInspectionExpiry,String status,String notes){}
 public record DriverRequest(@NotBlank String lastName,@NotBlank String firstName,String phone,@NotBlank String licenseNumber,LocalDate licenseExpiry,String status,String notes){}
 public record RouteRequest(@NotNull Long schoolYearId,@NotBlank String code,@NotBlank String name,Long vehicleId,Long driverId,LocalTime morningDeparture,LocalTime returnDeparture,Boolean active,String notes){}
 public record StopRequest(@NotBlank String name,@NotNull @Min(1) Integer stopOrder,LocalTime morningTime,LocalTime returnTime,String address,@DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,@DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,Boolean active){}
 public record AssignmentRequest(@NotNull Long studentEnrollmentId,@NotNull Long routeId,@NotNull Long stopId,@NotBlank String serviceType,@NotNull @DecimalMin("0") BigDecimal monthlyFee,@NotNull LocalDate startDate,LocalDate endDate){}
}
