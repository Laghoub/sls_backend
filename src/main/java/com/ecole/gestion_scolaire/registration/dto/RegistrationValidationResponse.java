package com.ecole.gestion_scolaire.registration.dto; import java.util.List; public record RegistrationValidationResponse(boolean valid,List<String> missingItems){}
