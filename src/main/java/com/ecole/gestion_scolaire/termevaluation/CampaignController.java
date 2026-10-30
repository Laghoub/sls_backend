package com.ecole.gestion_scolaire.termevaluation;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController
@RequestMapping("/api/term-evaluations/campaigns")
public class CampaignController {
 private final NamedParameterJdbcTemplate db;
 public CampaignController(NamedParameterJdbcTemplate db){this.db=db;}
 @GetMapping
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CAMPAGNE_GERER')")
 public List<Map<String,Object>> list(){
  return db.queryForList("SELECT * FROM term_evaluation_campaign ORDER BY opens_at DESC",Map.of());
 }
 @PostMapping
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CAMPAGNE_GERER')")
 public ResponseEntity<Map<String,Object>> create(@Valid @RequestBody CampaignRequest r){
  if(r.evaluationFrom().isAfter(r.evaluationTo()) || !r.opensAt().isBefore(r.closesAt()))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Dates incohérentes");
  if(r.cycleCode()!=null && !Set.of("PR","CO","LY").contains(r.cycleCode()))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cycle inconnu");
  var params=new MapSqlParameterSource()
   .addValue("year",r.schoolYearId()).addValue("title",r.title()).addValue("trimester",r.trimester())
   .addValue("cycle",r.cycleCode()).addValue("ef",r.evaluationFrom()).addValue("et",r.evaluationTo())
   .addValue("open",r.opensAt()).addValue("close",r.closesAt());
  var row=db.queryForMap("""
   INSERT INTO term_evaluation_campaign
   (school_year_id,title,trimester,cycle_code,evaluation_from,evaluation_to,opens_at,closes_at)
   VALUES (:year,:title,:trimester,:cycle,:ef,:et,:open,:close)
   RETURNING *
   """,params);
  return ResponseEntity.status(HttpStatus.CREATED).body(row);
 }
 @PatchMapping("/{id}/schedule")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CAMPAGNE_GERER')")
 public Map<String,Object> schedule(@PathVariable Long id){
  var found=db.queryForList("""
   UPDATE term_evaluation_campaign SET status='SCHEDULED',updated_at=now()
   WHERE id=:id AND status='DRAFT' RETURNING *
   """,Map.of("id",id));
  if(found.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Campagne absente ou statut incompatible");
  return found.get(0);
 }
}
