package com.ecole.gestion_scolaire.audit.dto;
import java.time.OffsetDateTime;
public record AuditLogResponse(Long id, OffsetDateTime occurredAt, Long userAccountId, String username,
 String userLastName, String userFirstName, String userRoles, String module, String action,
 String entityType, String entityId, String description, String oldValues, String newValues,
 String httpMethod, String requestPath, Integer httpStatus, String ipAddress, String requestId) {}
