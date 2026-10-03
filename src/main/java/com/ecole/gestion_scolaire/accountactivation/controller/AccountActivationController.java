package com.ecole.gestion_scolaire.accountactivation.controller;
import com.ecole.gestion_scolaire.accountactivation.dto.ActivationDtos.*; import com.ecole.gestion_scolaire.accountactivation.service.AccountActivationService; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/account-activation") public class AccountActivationController { private final AccountActivationService service; public AccountActivationController(AccountActivationService s){service=s;}
 @PostMapping @PreAuthorize("hasAuthority('COMPTE_ACTIVER')") public ActivateResponse activate(@RequestBody ActivateRequest r){return service.activate(r);}
 @PostMapping("/resend") @PreAuthorize("hasAuthority('COMPTE_ACTIVER')") public ActivateResponse resend(@RequestBody ActivateRequest r){return service.activate(r);}
 @GetMapping("/public/status") public TokenStatus status(@RequestParam String token){return service.status(token);}
 @PostMapping("/public/password") public ResponseEntity<Void> password(@RequestBody SetPasswordRequest r){service.setPassword(r);return ResponseEntity.noContent().build();}
}
