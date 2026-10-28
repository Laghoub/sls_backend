package com.ecole.gestion_scolaire.audit.controller;
import com.ecole.gestion_scolaire.audit.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/audit")
public class AuditController {
 private final AuditService service; public AuditController(AuditService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAuthority('AUDIT_CONSULTER')")
 public Object search(@RequestParam(required=false) String module,@RequestParam(required=false) String action,@RequestParam(required=false) Long userId,@RequestParam(required=false) String q,
 @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
 @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.search(module,action,userId,q,from,to,page,size);}
}
