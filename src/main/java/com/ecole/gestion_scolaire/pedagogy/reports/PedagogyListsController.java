package com.ecole.gestion_scolaire.pedagogy.reports;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/pedagogy/lists")
public class PedagogyListsController {
    private final PedagogyListsService service;
    private final PedagogyListsPdf pdf;
    public PedagogyListsController(PedagogyListsService service,PedagogyListsPdf pdf) {
        this.service=service;this.pdf=pdf;
    }
    @GetMapping("/students")
    @PreAuthorize("hasAuthority('ELEVE_CONSULTER')")
    public List<Map<String,Object>> students(@RequestParam(required=false) Long schoolYearId,
          @RequestParam(required=false) Long cycleId,@RequestParam(required=false) Long classId) {
        return service.students(schoolYearId,cycleId,classId);
    }
    @GetMapping("/teachers")
    @PreAuthorize("hasAuthority('ENSEIGNANT_CONSULTER')")
    public List<Map<String,Object>> teachers(@RequestParam(required=false) Long schoolYearId,
          @RequestParam(required=false) Long cycleId,@RequestParam(required=false) Long classId,
          @RequestParam(required=false) Long subjectId) {
        return service.teachers(schoolYearId,cycleId,classId,subjectId);
    }
    @GetMapping(value="/students/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('ELEVE_CONSULTER')")
    public ResponseEntity<byte[]> studentsPdf(@RequestParam(required=false) Long schoolYearId,
          @RequestParam(required=false) Long cycleId,@RequestParam(required=false) Long classId) throws IOException {
        return download("liste_eleves.pdf",pdf.render("LISTE DES ELEVES",service.students(schoolYearId,cycleId,classId),false));
    }
    @GetMapping(value="/teachers/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('ENSEIGNANT_CONSULTER')")
    public ResponseEntity<byte[]> teachersPdf(@RequestParam(required=false) Long schoolYearId,
          @RequestParam(required=false) Long cycleId,@RequestParam(required=false) Long classId,
          @RequestParam(required=false) Long subjectId) throws IOException {
        return download("liste_enseignants.pdf",pdf.render("LISTE DES ENSEIGNANTS",service.teachers(schoolYearId,cycleId,classId,subjectId),true));
    }
    private ResponseEntity<byte[]> download(String filename,byte[] bytes) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"")
              .contentType(MediaType.APPLICATION_PDF).body(bytes);
    }
}
