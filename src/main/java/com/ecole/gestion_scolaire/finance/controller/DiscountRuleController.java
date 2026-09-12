package com.ecole.gestion_scolaire.finance.controller;
import com.ecole.gestion_scolaire.finance.dto.discount.*;import com.ecole.gestion_scolaire.finance.service.DiscountRuleService;import jakarta.validation.Valid;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/finance/discount-rules") public class DiscountRuleController{private final DiscountRuleService s;public DiscountRuleController(DiscountRuleService s){this.s=s;}
 @GetMapping @PreAuthorize("hasAuthority('REDUCTION_CONSULTER')") public Object list(@RequestParam Long schoolYearId){return s.list(schoolYearId);}
 @PostMapping @PreAuthorize("hasAuthority('REDUCTION_PARAMETRER')") public DiscountRuleResponse create(@Valid @RequestBody DiscountRuleRequest r){return s.create(r);}
 @PutMapping("/{id}") @PreAuthorize("hasAuthority('REDUCTION_PARAMETRER')") public DiscountRuleResponse update(@PathVariable Long id,@Valid @RequestBody DiscountRuleRequest r){return s.update(id,r);}
 @PostMapping("/{id}/apply") @PreAuthorize("hasAuthority('REDUCTION_VALIDER')") public DiscountApplicationResponse apply(@PathVariable Long id){return s.apply(id);}
}
