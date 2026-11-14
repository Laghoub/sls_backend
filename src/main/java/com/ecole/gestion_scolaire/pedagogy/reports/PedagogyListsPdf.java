package com.ecole.gestion_scolaire.pedagogy.reports;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

@Component
public class PedagogyListsPdf {
    private static String printable(Object value) {
        String text=Normalizer.normalize(Objects.toString(value,""),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").replaceAll("[^\\x20-\\x7E]","?");
        return text.length()>75 ? text.substring(0,72)+"..." : text;
    }
    private static void draw(PDPageContentStream stream, PDFont font, int size, float x,float y,Object text) throws IOException {
        stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,y);
        stream.showText(printable(text));stream.endText();
    }
    public byte[] render(String title,List<Map<String,Object>> rows,boolean teacher) throws IOException {
        try(PDDocument doc=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            PDFont normal=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            int perPage=34, total=Math.max(1,(rows.size()+perPage-1)/perPage);
            for(int pageNo=0;pageNo<total;pageNo++) {
                PDPage page=new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));doc.addPage(page);
                try(PDPageContentStream cs=new PDPageContentStream(doc,page)) {
                    draw(cs,bold,14,38,552,"GROUPE SCOLAIRE PRIVE SCIENCES ET LETTRES");
                    draw(cs,bold,12,38,529,title);
                    draw(cs,normal,9,38,510,"Edition : "+LocalDate.now()+"    Total : "+rows.size());
                    float y=487;
                    draw(cs,bold,9,40,y,"N");draw(cs,bold,9,70,y,"Matricule");
                    draw(cs,bold,9,200,y,"Nom");draw(cs,bold,9,325,y,"Prenom");
                    draw(cs,bold,9,450,y,"Classe");draw(cs,bold,9,565,y,teacher?"Matiere":"Naissance");
                    draw(cs,bold,9,710,y,"Cycle");y-=16;
                    for(int i=pageNo*perPage;i<Math.min(rows.size(),(pageNo+1)*perPage);i++) {
                        Map<String,Object> r=rows.get(i);
                        draw(cs,normal,8,40,y,i+1);draw(cs,normal,8,70,y,r.get("matricule"));
                        draw(cs,normal,8,200,y,r.get("nom"));draw(cs,normal,8,325,y,r.get("prenom"));
                        draw(cs,normal,8,450,y,r.get("classe"));
                        draw(cs,normal,8,565,y,r.get(teacher?"matiere":"date_naissance"));
                        draw(cs,normal,8,710,y,r.get("cycle"));y-=12;
                    }
                    draw(cs,normal,8,38,30,"Page "+(pageNo+1)+" / "+total);
                }
            }
            doc.save(out);return out.toByteArray();
        }
    }
}
