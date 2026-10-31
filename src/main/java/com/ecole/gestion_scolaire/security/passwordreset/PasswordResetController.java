package com.ecole.gestion_scolaire.security.passwordreset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {
    private final PasswordResetService service;
    public PasswordResetController(PasswordResetService service) { this.service=service; }

    public record RequestLink(@NotBlank String username) {}
    public record Confirm(@NotBlank String token, @NotBlank @Size(min=10,max=128) String password,
                          @NotBlank String confirmPassword) {}

    @PostMapping("/request")
    public ResponseEntity<Map<String,String>> request(@Valid @RequestBody RequestLink body) {
        service.request(body.username());
        return ResponseEntity.ok(Map.of("message","Si un compte actif et une adresse e-mail sont associés à cet identifiant, un lien de réinitialisation sera envoyé."));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String,String>> confirm(@Valid @RequestBody Confirm body) {
        service.confirm(body.token(),body.password(),body.confirmPassword());
        return ResponseEntity.ok(Map.of("message","Votre mot de passe a été modifié. Vous pouvez vous connecter."));
    }
}
