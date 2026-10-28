package com.ecole.gestion_scolaire.attendance.monthly;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
@RestController
@RequestMapping("/api/teacher-attendance/monthly")
public class MonthlyPointageController {
 private final MonthlyPointageService service;
 public MonthlyPointageController(MonthlyPointageService service){this.service=service;}
 @GetMapping
 @PreAuthorize("hasAuthority('POINTAGE_MENSUEL_CONSULTER')")
 public List<MonthlyPointageRow> rows(@RequestParam LocalDate from,@RequestParam LocalDate to,
  @RequestParam(required=false) Long schoolYearId,@RequestParam(required=false) String cycle,
  @RequestParam(required=false) Long classId,@RequestParam(required=false) Long teacherId,
  @RequestParam(required=false) Long subjectId) {
  return service.rows(from,to,schoolYearId,cycle,classId,teacherId,subjectId);
 }
 @GetMapping("/summary")
 @PreAuthorize("hasAuthority('POINTAGE_MENSUEL_CONSULTER')")
 public List<MonthlyPointageSummary> summary(@RequestParam LocalDate from,@RequestParam LocalDate to,
  @RequestParam(required=false) Long schoolYearId,@RequestParam(required=false) String cycle,
  @RequestParam(required=false) Long classId,@RequestParam(required=false) Long teacherId,
  @RequestParam(required=false) Long subjectId) {
  return service.summary(from,to,schoolYearId,cycle,classId,teacherId,subjectId);
 }
 @GetMapping(value="/export.pdf",produces=MediaType.APPLICATION_PDF_VALUE)
 @PreAuthorize("hasAuthority('POINTAGE_MENSUEL_EXPORTER')")
 public ResponseEntity<byte[]> pdf(@RequestParam LocalDate from,@RequestParam LocalDate to,
  @RequestParam(required=false) Long schoolYearId,@RequestParam(required=false) String cycle,
  @RequestParam(required=false) Long classId,@RequestParam(required=false) Long teacherId,
  @RequestParam(required=false) Long subjectId) {
  var summary=service.summary(from,to,schoolYearId,cycle,classId,teacherId,subjectId);
  byte[] pdf=MonthlyPointagePdf.render(from,to,cycle,summary);
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
   .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=pointage-enseignants.pdf")
   .body(pdf);
 }
 @GetMapping(value="/export.csv",produces="text/csv")
 @PreAuthorize("hasAuthority('POINTAGE_MENSUEL_EXPORTER')")
 public ResponseEntity<byte[]> csv(@RequestParam LocalDate from,@RequestParam LocalDate to,
  @RequestParam(required=false) Long schoolYearId,@RequestParam(required=false) String cycle,
  @RequestParam(required=false) Long classId,@RequestParam(required=false) Long teacherId,
  @RequestParam(required=false) Long subjectId) {
  var rows=service.rows(from,to,schoolYearId,cycle,classId,teacherId,subjectId);
  StringBuilder csv=new StringBuilder("\uFEFFEnseignant;Classe;Matière;Date;Heures prévues;Heures effectuées;Statut;Séance\\r\\n");
  for(var r:rows) {
   for(String cell:List.of(r.teacherName(),r.className(),r.subjectName(),r.date().toString(),
       r.plannedHours().toString(),r.performedHours().toString(),r.status(),r.sessionId().toString())) {
    csv.append('"').append(cell.replace("\"","\"\"")).append('"').append(';');
   }
   csv.setLength(csv.length()-1);csv.append("\\r\\n");
  }
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
   "attachment; filename=pointage-enseignants.csv")
   .body(csv.toString().getBytes(StandardCharsets.UTF_8));
 }
}
