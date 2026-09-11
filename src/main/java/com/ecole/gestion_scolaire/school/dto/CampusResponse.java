package com.ecole.gestion_scolaire.school.dto;

public record CampusResponse(

        Long id,

        String code,

        String name,

        String address,

        String phone,

        boolean active
) {
}