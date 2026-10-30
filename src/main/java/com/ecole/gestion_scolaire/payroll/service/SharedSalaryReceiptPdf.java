package com.ecole.gestion_scolaire.payroll.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Color;
import java.text.Normalizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Shared printable PDF receipt for teacher and non-teacher payroll payments. */
public final class SharedSalaryReceiptPdf {
 private SharedSalaryReceiptPdf() {}
 public static byte[] render(String category,String receiptNumber,String name,String matricule,
  String period,String date,String method,String reference,String net,String amount,String total,String remaining) throws IOException {
  try(PDDocument doc=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   PDPage page=new PDPage(PDRectangle.A4);doc.addPage(page);
   var regular=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
   var bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
   try(PDPageContentStream cs=new PDPageContentStream(doc,page)){
    cs.setNonStrokingColor(new Color(14,39,78));cs.addRect(0,755,595,87);cs.fill();
    try(InputStream in=SharedSalaryReceiptPdf.class.getResourceAsStream("/sls-logo.png")){
     if(in!=null){byte[] logo=in.readAllBytes();PDImageXObject img=PDImageXObject.createFromByteArray(doc,logo,"logo");float h=64f;float w=img.getWidth()*h/img.getHeight();cs.drawImage(img,42,766,Math.min(w,90),h);}
    }
    line(cs,bold,15,145,803,"GROUPE SCOLAIRE PRIVE SCIENCES ET LETTRES",true);
    line(cs,regular,11,145,779,"RECU DE PAIEMENT - "+category,true);
    cs.setStrokingColor(new Color(196,153,70));cs.setLineWidth(2);cs.moveTo(40,738);cs.lineTo(555,738);cs.stroke();
    line(cs,bold,13,45,704,"RECU N° : "+receiptNumber,false);
    String[][] fields={{"Beneficiaire",name},{"Matricule",matricule},{"Periode",period},{"Date du paiement",date},
     {"Mode de paiement",method},{"Reference",reference},{"Salaire net",net+" DA"},
     {"Montant verse",amount+" DA"},{"Total verse",total+" DA"},{"Reste a payer",remaining+" DA"}};
    float y=665;
    for(int i=0;i<fields.length;i++){
     if(i%2==0){cs.setNonStrokingColor(new Color(244,247,251));cs.addRect(40,y-10,515,31);cs.fill();}
     line(cs,bold,10,50,y+1,fields[i][0]+" :",false);
     line(cs,regular,10,215,y+1,fields[i][1],false);y-=35;
    }
    cs.setStrokingColor(new Color(196,153,70));cs.moveTo(40,268);cs.lineTo(555,268);cs.stroke();
    line(cs,regular,9,45,247,"Document emis par le Groupe Scolaire Prive Sciences et Lettres.",false);
    line(cs,regular,9,45,231,"Reimpression sans nouvel encaissement ni mouvement de caisse.",false);
   }
   doc.save(out);return out.toByteArray();
  }
 }
 private static void line(PDPageContentStream cs,org.apache.pdfbox.pdmodel.font.PDFont font,int size,float x,float y,String value,boolean white) throws IOException{
  cs.setNonStrokingColor(new Color(white?255:25,white?255:45,white?255:70));
  String safe=Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^\\x20-\\x7E]","?");
  if(safe.length()>68)safe=safe.substring(0,65)+"...";
  cs.beginText();cs.setFont(font,size);cs.newLineAtOffset(x,y);cs.showText(safe);cs.endText();
 }
}
