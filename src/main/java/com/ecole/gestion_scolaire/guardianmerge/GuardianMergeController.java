package com.ecole.gestion_scolaire.guardianmerge;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** API additive : aucune modification des API parents/finance existantes. */
@RestController
@RequestMapping("/api/guardians/merge")
public class GuardianMergeController {
    private final GuardianMergeService service;
    public GuardianMergeController(GuardianMergeService service) { this.service = service; }

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('RESPONSABLE_CONSULTER')")
    public Map<String, Object> preview(@RequestParam Long primaryId, @RequestParam Long duplicateId) {
        return service.preview(primaryId, duplicateId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESPONSABLE_FUSIONNER')")
    public Map<String, Object> merge(@RequestBody MergeRequest request) {
        return service.merge(request);
    }

    public record MergeRequest(Long primaryId, Long duplicateId, boolean confirmed) {}
}
