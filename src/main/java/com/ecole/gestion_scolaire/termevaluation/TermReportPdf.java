package com.ecole.gestion_scolaire.termevaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Renders the immutable publication snapshot as searchable, vector-text PDF pages. */
public final class TermReportPdf {
 private TermReportPdf() {}
 private static final ObjectMapper JSON=new ObjectMapper();
 private static final float W=595,H=842,M=43,RIGHT=W-M,BOTTOM=57;
 private static final Color NAVY=new Color(23,52,81), LIGHT=new Color(239,244,248), GOLD=new Color(197,154,74), GRAY=new Color(77,97,115);
 private static String val(JsonNode n,String key){JsonNode v=n.path(key);return v.isMissingNode()||v.isNull()?"":v.asText("");}
 private static String answer(JsonNode a){
  String option=val(a,"optionFr");if(option.isBlank())option=val(a,"option");if(option.isBlank())option=val(a,"optionAr");
  String free=val(a,"freeText");return option.isBlank()?free:free.isBlank()?option:option+" — "+free;
 }
 private static final Map<String,String> CRITERIA_FR=Map.ofEntries(
   Map.entry("BEHAVIOR","Comportement général"),
   Map.entry("RESPECT","Respect des enseignants"),
   Map.entry("RULES","Respect du règlement"),
   Map.entry("PEERS","Relations avec les camarades"),
   Map.entry("ATTENTION","Attention pendant les cours"),
   Map.entry("PARTICIPATION","Participation en classe"),
   Map.entry("EXERCISES","Réalisation des exercices"),
   Map.entry("HOMEWORK","Devoirs à domicile"),
   Map.entry("PREPARATION","Préparation des cours"),
   Map.entry("AUTONOMY","Autonomie"),
   Map.entry("EFFORT","Efforts fournis"),
   Map.entry("LEVEL","Appréciation du niveau scolaire"),
   Map.entry("DIFFICULTY","Difficultés constatées"),
   Map.entry("RECOMMENDATION","Recommandations")
 );
 private static String label(JsonNode a){
  String s=val(a,"labelFr");
  if(!s.isBlank())return s;
  s=CRITERIA_FR.get(val(a,"criterion"));
  if(s!=null)return s;
  // Never render an Arabic-only label in the French report.
  return "Critère : "+val(a,"criterion");
 }
 private static String publicationDate(String date){
  if(date==null||date.isBlank()||"null".equalsIgnoreCase(date))return "Non renseignée";
  try{return LocalDateTime.parse(date.replace(' ','T')).format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));}
  catch(Exception ignored){return date.replace('T',' ');}
 }
 private static Path locateFont(){
  String override=System.getProperty("sls.pdf.font");
  List<String> choices=new ArrayList<>();if(override!=null)choices.add(override);
  Collections.addAll(choices,"C:/Windows/Fonts/arial.ttf","C:/Windows/Fonts/ARIAL.TTF","C:/Windows/Fonts/segoeui.ttf",
   "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf","/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf");
  for(String f:choices)if(Files.isRegularFile(Path.of(f)))return Path.of(f);
  throw new IllegalStateException("Police TrueType introuvable. Configurer -Dsls.pdf.font=C:/Windows/Fonts/arial.ttf");
 }
 private static final class Painter implements AutoCloseable{
  final PDDocument doc=new PDDocument();final PDFont font;PDPage page;PDPageContentStream cs;float y;int pageNo=0;
  Painter() throws IOException {try(InputStream in=Files.newInputStream(locateFont())){font=PDType0Font.load(doc,in);}newPage();}
  void newPage()throws IOException{
   if(cs!=null)cs.close();page=new PDPage(PDRectangle.A4);doc.addPage(page);cs=new PDPageContentStream(doc,page);pageNo++;
   try(InputStream in=TermReportPdf.class.getResourceAsStream("/sls-logo.png")){
    if(in!=null){BufferedImage logo=ImageIO.read(in);if(logo!=null){PDImageXObject x=LosslessFactory.createFromImage(doc,logo);cs.drawImage(x,M,H-98,53,53);}}
   }catch(Exception ignored){}
   line("GROUPE SCOLAIRE PRIVÉ SCIENCES ET LETTRES",M+66,H-65,11,true,NAVY);
   line("BILAN D'ÉVALUATION TRIMESTRIELLE",M,H-131,16,true,NAVY);
   rect(M,H-155,RIGHT-M,1.2f,GOLD);
   rect(M,40,RIGHT-M,1,GOLD);
   line("Sciences et Lettres · Bilan trimestriel",M,26,7,false,GRAY);
   line("Page "+pageNo,RIGHT-38,26,7,false,GRAY);
   y=H-178;
  }
  float width(String s,float size)throws IOException{return font.getStringWidth(s)*size/1000f;}
  String safe(String s){
   if(s==null)return "";
   // Keep source data unchanged, replacing only PDF-unsupported control characters.
   return s.replace('\t',' ').replace('\r',' ').replaceAll("[\\p{Cntrl}&&[^\\n]]","");
  }
  void line(String s,float x,float baseline,float size,boolean bold,Color color)throws IOException{
   s=safe(s).replace('\n',' ');if(s.isEmpty())return;
   // PDFBox embeds the TrueType font and preserves searchable/selectable text.
   cs.beginText();cs.setFont(font,size);cs.setNonStrokingColor(color);
   cs.newLineAtOffset(x,baseline);cs.showText(s);cs.endText();
   
  }
  void rect(float x,float bottom,float w,float h,Color color)throws IOException{cs.setNonStrokingColor(color);cs.addRect(x,bottom,w,h);cs.fill();}
  List<String> wrap(String value,float width,float size)throws IOException{
   List<String> result=new ArrayList<>();
   for(String para:safe(value).split("\\n",-1)){
    if(para.isBlank()){result.add(" ");continue;}
    StringBuilder line=new StringBuilder();
    for(String word:para.split("\\s+")){
     String candidate=line.isEmpty()?word:line+" "+word;
     if(width(candidate,size)<=width){line.setLength(0);line.append(candidate);continue;}
     if(!line.isEmpty()){result.add(line.toString());line.setLength(0);}
     StringBuilder part=new StringBuilder();
     for(int i=0;i<word.length();i++){
      String next=part.toString()+word.charAt(i);
      if(!part.isEmpty()&&width(next,size)>width){result.add(part.toString());part.setLength(0);}
      part.append(word.charAt(i));
     }
     line.append(part);
    }
    if(!line.isEmpty())result.add(line.toString());
   }
   return result;
  }
  void ensure(float height)throws IOException{if(y-height<BOTTOM)newPage();}
  void heading(String text)throws IOException{ensure(30);line(text,M,y,10.5f,true,NAVY);rect(M,y-3,Math.min(width(text,10.5f),RIGHT-M),.6f,NAVY);y-=21;}
  void subject(String title)throws IOException{ensure(32);rect(M,y-22,RIGHT-M,25,NAVY);line(title,M+9,y-14,9.2f,true,Color.WHITE);y-=30;}
  void field(String label,String value,int index)throws IOException{
   List<String> labels=wrap(label,174,8.2f),values=wrap(value.isBlank()?"—":value,RIGHT-M-205,8.4f);
   int li=0,vi=0;boolean continuation=false;
   do{
    int remaining=Math.max(labels.size()-li,values.size()-vi);
    int capacity=Math.max(1,(int)((y-BOTTOM-10)/10.5f));
    if(capacity<1){newPage();continue;}
    int lines=Math.min(remaining,capacity);float height=Math.max(19,lines*10.5f+7);
    if(y-height<BOTTOM){newPage();continue;}
    if(index%2==0)rect(M,y-height,RIGHT-M,height,LIGHT);
    for(int i=0;i<lines;i++){
     if(li<labels.size())line(labels.get(li++),M+7,y-12-i*10.5f,8.2f,false,NAVY);
     if(vi<values.size())line(values.get(vi++),M+201,y-12-i*10.5f,8.4f,false,NAVY);
    }
    rect(M,y-height,RIGHT-M,.45f,new Color(217,226,235));y-=height;continuation=true;
   }while(li<labels.size()||vi<values.size());
  }
  void grade(String subject,String title,String date,String score,int index)throws IOException{
   List<String> a=wrap(subject,130,8.4f),b=wrap(title,185,8.4f),c=wrap(date,75,8.4f),d=wrap(score,65,8.4f);
   int rows=Math.max(Math.max(a.size(),b.size()),Math.max(c.size(),d.size()));int start=0;
   while(start<rows){ensure(29);int take=Math.max(1,Math.min(rows-start,(int)((y-BOTTOM-11)/12)));
    float height=Math.max(27,take*12+9);if(index%2==0)rect(M,y-height,RIGHT-M,height,LIGHT);
    for(int j=0;j<take;j++){int k=start+j;float yy=y-15-j*12;
     if(k<a.size())line(a.get(k),M+6,yy,8.4f,false,NAVY);
     if(k<b.size())line(b.get(k),M+142,yy,8.4f,false,NAVY);
     if(k<c.size())line(c.get(k),M+340,yy,8.4f,false,NAVY);
     if(k<d.size())line(d.get(k),M+425,yy,8.4f,false,NAVY);
    }y-=height;start+=take;
   }
  }
  byte[] bytes()throws IOException{cs.close();cs=null;ByteArrayOutputStream out=new ByteArrayOutputStream();doc.save(out);return out.toByteArray();}
  @Override public void close()throws IOException{if(cs!=null)cs.close();doc.close();}
 }
 public static byte[] render(String snapshot)throws IOException{return render(snapshot,"");}
 public static byte[] render(String snapshot,String publishedAt)throws IOException{
  JsonNode root=JSON.readTree(snapshot);
  if(val(root,"studentName").isBlank()||val(root,"className").isBlank())
   throw new IOException("Snapshot incomplet : élève ou classe absent. Republier le rapport depuis l’administration.");
  try(Painter p=new Painter()){
   p.line("Élève : "+val(root,"studentName"),M,p.y,11,true,NAVY);p.y-=19;
   p.line("Classe : "+val(root,"className"),M,p.y,10,false,NAVY);p.y-=30;
   p.heading("1. APPRÉCIATIONS PAR MATIÈRE");
   for(JsonNode ev:root.path("evaluations")){
    String subject=val(ev,"subjectName");if(subject.isBlank())throw new IOException("Matière absente du snapshot");
    p.subject(subject);int index=0;
    // One row per original question/criterion, with its corresponding teacher answer.
    for(JsonNode a:ev.path("answers")){
     String criterion=label(a),response=answer(a);
     if(!criterion.isBlank()||!response.isBlank())p.field(criterion,response,index++);
    }
    String[][] extra={{"Conseils pour progresser",val(ev,"recommendations")},{"Difficultés",val(ev,"difficulties")},{"Observations de l'enseignant",val(ev,"observation")}};
    for(String[] row:extra)if(!row[1].isBlank())p.field(row[0],row[1],index++);
    p.y-=8;
   }
   p.heading("2. NOTES ET ÉVALUATIONS");
   p.grade("Matière","Évaluation / type","Date","Note",0);
   int i=1;
   for(JsonNode g:root.path("grades")){
    String title=val(g,"title"),type=val(g,"type");
    p.grade(val(g,"subjectName"),title+(type.isBlank()?"":" / "+type),val(g,"assessmentDate"),val(g,"score")+" / "+val(g,"maxScore"),i++);
   }
   p.y-=8;p.heading("3. VALIDATION");p.ensure(55);
   p.line("Signé électroniquement par Nassim LAGHOUB, conseiller général",M,p.y,9.4f,true,NAVY);p.y-=22;
   p.line("Date de publication : "+publicationDate(publishedAt),M,p.y,9,false,NAVY);
   return p.bytes();
  }catch(IllegalArgumentException e){throw new IOException("Erreur de police PDF : "+e.getMessage(),e);}
 }
}
