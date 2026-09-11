package com.ecole.gestion_scolaire.common.exception;

import java.time.OffsetDateTime;

public record ApiErrorResponse(

        OffsetDateTime timestamp,

        int status,

        String code,

        String message,

        String path
) {
}