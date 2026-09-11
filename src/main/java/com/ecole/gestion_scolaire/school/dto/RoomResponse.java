package com.ecole.gestion_scolaire.school.dto;

public record RoomResponse(

        Long id,

        Long campusId,

        String campusCode,

        String campusName,

        String code,

        String name,

        Integer capacity,

        String roomType,

        boolean active
) {
}