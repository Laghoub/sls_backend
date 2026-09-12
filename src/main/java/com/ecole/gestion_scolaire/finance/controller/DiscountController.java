package com.ecole.gestion_scolaire.finance.controller;
import com.ecole.gestion_scolaire.finance.dto.discount.*;import com.ecole.gestion_scolaire.finance.service.DiscountService;import jakarta.validation.Valid;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/finance/discounts") public class DiscountController{private final DiscountService s;public DiscountController(DiscountService s){this.s=s;}
 @GetMapping @PreAuthorize("hasAuthority('REDUCTION_CONSULTER')") public Object list(@RequestParam(required=false)String status,@RequestParam(required=false)Long enrollmentId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return s.search(status,enrollmentId,page,size);}
 @PostMapping @PreAuthorize("hasAuthority('REDUCTION_CREER')") public StudentDiscountResponse create(@Valid @RequestBody StudentDiscountRequest r){return s.create(r);}
 @PostMapping("/{id}/approve") @PreAuthorize("hasAuthority('REDUCTION_VALIDER')") public StudentDiscountResponse approve(@PathVariable Long id){return s.approve(id);}
 @PostMapping("/{id}/reject") @PreAuthorize("hasAuthority('REDUCTION_REFUSER')") public StudentDiscountResponse reject(@PathVariable Long id,@Valid @RequestBody DiscountRejectRequest r){return s.reject(id,r.reason());}
 @PostMapping("/enrollments/{id}/reapply") @PreAuthorize("hasAuthority('REDUCTION_VALIDER')") public DiscountApplicationResponse reapply(@PathVariable Long id){return s.reapplyEnrollment(id);}
}
