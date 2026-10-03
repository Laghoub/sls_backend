package com.ecole.gestion_scolaire.accountactivation.dto;
public final class ActivationDtos { private ActivationDtos(){}
 public record ActivateRequest(Long personId,String matricule,String roleCode){}
 public record ActivateResponse(Long userAccountId,String matricule,String email,String status,boolean emailSent){}
 public record TokenStatus(boolean valid,String matricule,String firstName,String lastName){}
 public record SetPasswordRequest(String token,String password,String confirmPassword){}
}
