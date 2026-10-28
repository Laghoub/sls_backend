package com.ecole.gestion_scolaire.attendance.monthly;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.*;

/**
 * PDF A3 paysage sans dépendance externe.
 * Police PDF Helvetica WinAnsi : caractères latins uniquement.
 */
public final class MonthlyPointagePdf {
 private static final Charset CP1252=Charset.forName("windows-1252");
 private static final float W=1190.55f,H=841.89f;
 private MonthlyPointagePdf() {}

 public static byte[] render(LocalDate from,LocalDate to,String cycle,
                             List<MonthlyPointageSummary> rows) {
  List<LocalDate> days=new ArrayList<>();
  for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1))days.add(d);
  // Au-delà de 31 jours, découpage horizontal en tranches de 31 jours.
  List<String> pages=new ArrayList<>();
  int slices=Math.max(1,(days.size()+30)/31);
  for(int slice=0;slice<slices;slice++) {
   List<LocalDate> chunk=days.subList(slice*31,Math.min(days.size(),(slice+1)*31));
   int perPage=34;
   int pageCount=Math.max(1,(rows.size()+perPage-1)/perPage);
   for(int pg=0;pg<pageCount;pg++){
    StringBuilder c=new StringBuilder();
    c.append("q 510 0 0 88 35 ").append(H-100).append(" cm /Header Do Q\n");
    c.append("0.12 0.20 0.34 rg\n");
    text(c,570,H-53,11,"Période : "+from+" au "+to+"    Cycle : "+(cycle==null?"Tous":cycle));
    text(c,570,H-72,9,"Heures validées par enseignant et par classe.");
    float top=H-115;
    float teacherX=36,classX=194,dayX=356,dayWidth=23.8f;
    float totalX=dayX+chunk.size()*dayWidth;
    rect(c,35,top-23,Math.min(W-70,totalX+61-35),25,true);
    c.append("1 1 1 rg\n");
    text(c,40,top-15,9,"ENSEIGNANT");
    text(c,classX+4,top-15,9,"CLASSE");
    for(int i=0;i<chunk.size();i++)text(c,dayX+i*dayWidth+3,top-15,8,
      String.format("%02d",chunk.get(i).getDayOfMonth()));
    text(c,totalX+5,top-15,9,"TOTAL");
    c.append("0.12 0.20 0.34 rg\n");
    int end=Math.min(rows.size(),(pg+1)*perPage);
    for(int r=pg*perPage;r<end;r++) {
     var row=rows.get(r);
     float y=top-24-(r-pg*perPage)*18;
     if(r%2==0){c.append("0.93 0.95 0.97 rg\n");rect(c,35,y-18,Math.min(W-70,totalX+61-35),18,true);}
     c.append("0.12 0.20 0.34 rg\n");
     text(c,40,y-12,8,cut(row.teacherName(),29));
     text(c,classX+4,y-12,8,cut(row.className(),27));
     for(int i=0;i<chunk.size();i++) {
      var hours=row.hoursByDay().get(chunk.get(i));
      if(hours!=null && hours.signum()!=0)
       text(c,dayX+i*dayWidth+2,y-12,7,hours.stripTrailingZeros().toPlainString());
     }
     text(c,totalX+5,y-12,8,row.totalHours().stripTrailingZeros().toPlainString());
    }
    c.append("0.12 0.20 0.34 rg\n");
    text(c,36,58,8,"Attention : les heures de classes regroupées peuvent figurer sur plusieurs lignes.");
    text(c,36,43,8,"Les totaux par classe ne doivent pas être additionnés pour calculer une paie sans dédoublonnage.");
    text(c,W-160,43,8,"Page "+(pg+1)+"/"+pageCount+" - Série "+(slice+1)+"/"+slices);
    pages.add(c.toString());
   }
  }
  return pdf(pages);
 }
 private static String cut(String s,int n) {if(s==null)return "";return s.length()>n?s.substring(0,n-2)+"..":s;}
 private static String esc(String s){
  StringBuilder b=new StringBuilder();
  for(char ch:s.toCharArray()) {
   if(ch=='\\'||ch=='('||ch==')')b.append('\\');
   b.append(ch<=255?ch:'?');
  }
  return b.toString();
 }
 private static void text(StringBuilder c,float x,float y,int size,String s) {
  c.append("BT /F1 ").append(size).append(" Tf ").append(x).append(' ').append(y)
   .append(" Td (").append(esc(s)).append(") Tj ET\n");
 }
 private static void rect(StringBuilder c,float x,float y,float w,float h,boolean fill){
  c.append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h)
   .append(fill?" re f\n":" re S\n");
 }
 private static byte[] headerImage() throws Exception {
  BufferedImage img=new BufferedImage(1020,176,BufferedImage.TYPE_INT_RGB);
  Graphics2D g=img.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  g.setColor(Color.WHITE);g.fillRect(0,0,1020,176);
  try(var in=MonthlyPointagePdf.class.getResourceAsStream("/sls-logo.png")) {
   if(in!=null) {
    BufferedImage logo=ImageIO.read(in);
    g.drawImage(logo,8,6,168,150,null);
   }
  }
  g.setColor(new Color(27,55,91));
  g.setFont(new Font("SansSerif",Font.BOLD,27));
  g.drawString("GROUPE SCOLAIRE PRIVÉ SCIENCES ET LETTRES",186,51);
  g.setFont(new Font("SansSerif",Font.PLAIN,24));
  // Java2D assure la mise en forme Unicode du texte arabe.
  String arabic="المجمع المدرسي الخاص علوم وآداب";
  g.drawString(arabic,186,93);
  g.setFont(new Font("SansSerif",Font.BOLD,23));
  g.drawString("POINTAGE MENSUEL DES ENSEIGNANTS",186,136);
  g.dispose();
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  ImageIO.write(img,"jpg",bytes);
  return bytes.toByteArray();
 }
 private static byte[] pdf(List<String> pages) {
  try {
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   List<Integer> offsets=new ArrayList<>();offsets.add(0);
   write(out,"%PDF-1.4\n");
   byte[] header=headerImage();
   int n=4+pages.size()*2;
   writeObj(out,offsets,1,"<< /Type /Catalog /Pages 2 0 R >>");
   StringBuilder kids=new StringBuilder();
   for(int i=0;i<pages.size();i++)kids.append(5+i*2).append(" 0 R ");
   writeObj(out,offsets,2,"<< /Type /Pages /Kids ["+kids+"] /Count "+pages.size()+" >>");
   writeObj(out,offsets,3,"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
   offsets.add(out.size());
   write(out,"4 0 obj\n<< /Type /XObject /Subtype /Image /Width 1020 /Height 176 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "+header.length+" >>\nstream\n");
   out.write(header);write(out,"\nendstream\nendobj\n");
   for(int i=0;i<pages.size();i++){
    int pageId=5+i*2,contentId=pageId+1;
    writeObj(out,offsets,pageId,"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "+W+" "+H+
      "] /Resources << /Font << /F1 3 0 R >> /XObject << /Header 4 0 R >> >> /Contents "+contentId+" 0 R >>");
    byte[] data=pages.get(i).getBytes(CP1252);
    offsets.add(out.size());write(out,contentId+" 0 obj\n<< /Length "+data.length+" >>\nstream\n");
    out.write(data);write(out,"\nendstream\nendobj\n");
   }
   int xref=out.size();
   write(out,"xref\n0 "+(n+1)+"\n0000000000 65535 f \n");
   for(int i=1;i<=n;i++)write(out,String.format(Locale.ROOT,"%010d 00000 n \n",offsets.get(i)));
   write(out,"trailer\n<< /Size "+(n+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n");
   return out.toByteArray();
  }catch(Exception ex){throw new IllegalStateException("Erreur génération PDF",ex);}
 }
 private static void writeObj(ByteArrayOutputStream out,List<Integer> offsets,int id,String value){
  offsets.add(out.size());write(out,id+" 0 obj\n"+value+"\nendobj\n");
 }
 private static void write(ByteArrayOutputStream out,String s){
  try{out.write(s.getBytes(CP1252));}catch(Exception e){throw new IllegalStateException(e);}
 }
}
