package com.ecole.gestion_scolaire.student.dto;

import com.ecole.gestion_scolaire.identity.dto.PersonResponse;

public record GuardianWithPersonResponse(

        GuardianResponse guardian,

        PersonResponse person
) {
}