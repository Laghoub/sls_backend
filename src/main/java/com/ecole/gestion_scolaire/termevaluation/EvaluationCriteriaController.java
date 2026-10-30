package com.ecole.gestion_scolaire.termevaluation;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/term-evaluations/criteria")
public class EvaluationCriteriaController {
 private final NamedParameterJdbcTemplate db;
 public EvaluationCriteriaController(NamedParameterJdbcTemplate db){this.db=db;}
 @GetMapping
 @PreAuthorize("hasAnyAuthority('EVALUATION_TRIMESTRIELLE_SAISIR','EVALUATION_TRIMESTRIELLE_CONTROLER','EVALUATION_TRIMESTRIELLE_CAMPAGNE_GERER')")
 public List<Map<String,Object>> criteria(@RequestParam(required=false) String cycle){
  if(cycle!=null && !Set.of("PR","CO","LY").contains(cycle))
   throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Cycle inconnu");
  return db.queryForList("""
   SELECT cr.id,cr.code,cr.section,cr.label_fr,cr.label_ar,cr.cycle_code,cr.sort_order,
    COALESCE((SELECT jsonb_agg(jsonb_build_object('id',o.id,'code',o.code,'labelFr',o.label_fr,
     'labelAr',o.label_ar) ORDER BY o.sort_order,o.id)
     FROM term_evaluation_option o WHERE o.criterion_id=cr.id),'[]'::jsonb)::text AS options_json
   FROM term_evaluation_criterion cr
   WHERE cr.active=TRUE AND (cr.cycle_code IS NULL OR cr.cycle_code=:cycle)
   ORDER BY cr.section,cr.sort_order,cr.id
   """,new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("cycle",cycle));
 }
}
