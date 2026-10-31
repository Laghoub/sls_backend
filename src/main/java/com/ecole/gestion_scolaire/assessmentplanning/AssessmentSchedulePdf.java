package com.ecole.gestion_scolaire.assessmentplanning;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.text.Normalizer;
import java.util.*;

/** A4 portrait : deux exemplaires A5 identiques, séparés par un trait de coupe. */
@Component
public class AssessmentSchedulePdf {
 private static String safe(Object v){String s=Normalizer.normalize(Objects.toString(v,""),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^\\x20-\\x7E]","?");return s.length()>75?s.substring(0,72)+"...":s;}
 private static void txt(PDPageContentStream cs,PDFont f,int size,float x,float y,Object value)throws IOException{cs.beginText();cs.setFont(f,size);cs.newLineAtOffset(x,y);cs.showText(safe(value));cs.endText();}
 private static void copy(PDPageContentStream cs,Map<String,Object> s,List<Map<String,Object>> entries,float top)throws IOException{
  PDFont bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),regular=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  txt(cs,bold,12,52,top-25,"GROUPE SCOLAIRE PRIVE SCIENCES ET LETTRES");
  txt(cs,bold,11,52,top-46,"EMPLOI DU TEMPS DES "+("EXAMEN".equals(s.get("assessment_type"))?"EXAMENS":"DEVOIRS"));
  txt(cs,regular,9,52,top-64,"Classe : "+safe(s.get("class_name"))+"   Trimestre : "+s.get("term"));
  txt(cs,regular,9,52,top-78,"Annee scolaire : "+safe(s.get("school_year")));
  float y=top-101;cs.setLineWidth(.5f);cs.moveTo(48,y+11);cs.lineTo(547,y+11);cs.stroke();
  txt(cs,bold,9,52,y,"Date");txt(cs,bold,9,145,y,"Horaire");txt(cs,bold,9,263,y,"Matiere");
  y-=21;int n=0;
  for(Map<String,Object> e:entries){if(n++>=10){txt(cs,regular,8,52,y,"Suite du calendrier : consulter la version numerique.");break;}
   String hours=safe(e.get("start_time"));if(hours.length()>5)hours=hours.substring(0,5);String end=safe(e.get("end_time"));if(end.length()>5)end=end.substring(0,5);
   txt(cs,regular,9,52,y,e.get("assessment_date"));txt(cs,regular,9,145,y,hours+" - "+end);txt(cs,regular,9,263,y,e.get("subject_name"));
   cs.moveTo(48,y-7);cs.lineTo(547,y-7);cs.stroke();y-=23;
  }
 }
 public byte[] render(Map<String,Object> s,List<Map<String,Object>> entries)throws IOException{
  try(PDDocument doc=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   // Paginer par 10 lignes : 2 tableaux identiques sur chaque feuille A4.
   for(int offset=0;offset<Math.max(1,entries.size());offset+=10){List<Map<String,Object>> pageEntries=entries.subList(Math.min(offset,entries.size()),Math.min(offset+10,entries.size()));PDPage page=new PDPage(PDRectangle.A4);doc.addPage(page);
    try(PDPageContentStream cs=new PDPageContentStream(doc,page)){
     copy(cs,s,pageEntries,815);copy(cs,s,pageEntries,405);
     cs.setLineDashPattern(new float[]{5,4},0);cs.setLineWidth(.6f);cs.moveTo(35,421);cs.lineTo(560,421);cs.stroke();
    }
   }
   doc.save(out);return out.toByteArray();
  }
 }
}
