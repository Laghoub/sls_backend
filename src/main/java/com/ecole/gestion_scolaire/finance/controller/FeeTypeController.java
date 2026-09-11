package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.fee.*;
import com.ecole.gestion_scolaire.finance.service.FeeTypeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/fee-types")
public class FeeTypeController {
    private final FeeTypeService s;

    public FeeTypeController(FeeTypeService x) {
        s = x;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TARIF_CONSULTER')")
    public PageResponse<FeeTypeResponse> all(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.search(search, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TARIF_GERER')")
    public FeeTypeResponse create(@Valid @RequestBody FeeTypeRequest r) {
        return s.create(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TARIF_GERER')")
    public FeeTypeResponse update(@PathVariable Long id, @Valid @RequestBody FeeTypeRequest r) {
        return s.update(id, r);
    }
}
