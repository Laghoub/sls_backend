package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.tariff.*;
import com.ecole.gestion_scolaire.finance.service.TariffService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/tariffs")
public class TariffController {
    private final TariffService s;

    public TariffController(TariffService x) {
        s = x;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TARIF_CONSULTER')")
    public PageResponse<TariffResponse> all(@RequestParam(required = false) Long schoolYearId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.search(schoolYearId, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TARIF_GERER')")
    public TariffResponse create(@Valid @RequestBody TariffRequest r) {
        return s.create(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TARIF_GERER')")
    public TariffResponse update(@PathVariable Long id, @Valid @RequestBody TariffRequest r) {
        return s.update(id, r);
    }
}
