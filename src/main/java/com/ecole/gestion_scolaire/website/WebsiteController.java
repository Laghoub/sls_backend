package com.ecole.gestion_scolaire.website;

import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/website")
public class WebsiteController {
 private final NamedParameterJdbcTemplate db;
 private final Path mediaRoot;
 public WebsiteController(NamedParameterJdbcTemplate db, @Value("${app.website.media-dir:./website-media}") String mediaDir) {
  this.db=db; this.mediaRoot=Paths.get(mediaDir).toAbsolutePath().normalize();
 }
 private static final Map<String,Object> NONE=Map.of();
 private List<Map<String,Object>> list(String sql,Map<String,?> params){return db.queryForList(sql,params);}
 @GetMapping("/public/content") public List<Map<String,Object>> content(){return list("SELECT content_key,title_fr,title_ar,body_fr,body_ar,image_url FROM website_content WHERE published=true ORDER BY id",NONE);}
 @GetMapping("/public/news") public List<Map<String,Object>> news(){return list("SELECT id,title_fr,title_ar,body_fr,body_ar,image_url,published_at FROM website_news WHERE published=true AND (NULLIF(BTRIM(title_fr),'') IS NOT NULL OR NULLIF(BTRIM(title_ar),'') IS NOT NULL OR NULLIF(BTRIM(body_fr),'') IS NOT NULL OR NULLIF(BTRIM(body_ar),'') IS NOT NULL) ORDER BY published_at DESC NULLS LAST,id DESC",NONE);}
 @GetMapping("/public/gallery") public List<Map<String,Object>> gallery(){return list("SELECT id,title_fr,title_ar,image_url,category,sort_order FROM website_gallery WHERE published=true AND NULLIF(BTRIM(image_url),'') IS NOT NULL ORDER BY sort_order,id",NONE);}
 @GetMapping("/public/media/{name:.+}") public ResponseEntity<byte[]> media(@PathVariable String name) throws Exception {
  if(!name.matches("[a-f0-9-]{36}\\.(jpg|png|webp)")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  Path file=mediaRoot.resolve(name).normalize();if(!file.startsWith(mediaRoot)||!Files.isRegularFile(file))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  String ext=name.substring(name.lastIndexOf('.')+1);MediaType type=ext.equals("png")?MediaType.IMAGE_PNG:ext.equals("webp")?MediaType.parseMediaType("image/webp"):MediaType.IMAGE_JPEG;
  return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7))).body(Files.readAllBytes(file));
 }
 @GetMapping("/admin/content") @PreAuthorize("hasAuthority('SITE_WEB_CONSULTER')") public List<Map<String,Object>> adminContent(){return list("SELECT * FROM website_content ORDER BY id",NONE);}
 @GetMapping("/admin/news") @PreAuthorize("hasAuthority('SITE_WEB_CONSULTER')") public List<Map<String,Object>> adminNews(){return list("SELECT * FROM website_news ORDER BY id DESC",NONE);}
 @GetMapping("/admin/gallery") @PreAuthorize("hasAuthority('SITE_WEB_CONSULTER')") public List<Map<String,Object>> adminGallery(){return list("SELECT * FROM website_gallery ORDER BY sort_order,id",NONE);}
 @PutMapping("/admin/content/{key}") @PreAuthorize("hasAuthority('SITE_WEB_MODIFIER')") public void saveContent(@PathVariable String key,@RequestBody Content input){
  int n=db.update("UPDATE website_content SET title_fr=COALESCE(NULLIF(:tf,''),title_fr),title_ar=COALESCE(NULLIF(:ta,''),title_ar),body_fr=COALESCE(NULLIF(:bf,''),body_fr),body_ar=COALESCE(NULLIF(:ba,''),body_ar),image_url=COALESCE(NULLIF(:img,''),image_url),published=:pub,updated_at=NOW() WHERE content_key=:key",new MapSqlParameterSource().addValue("tf",input.titleFr).addValue("ta",input.titleAr).addValue("bf",input.bodyFr).addValue("ba",input.bodyAr).addValue("img",input.imageUrl).addValue("pub",input.published).addValue("key",key));
  if(n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
 }
 @PostMapping("/admin/news") @PreAuthorize("hasAuthority('SITE_WEB_MODIFIER')") public Map<String,Object> addNews(@RequestBody News input){
  Long id=db.queryForObject("INSERT INTO website_news(title_fr,title_ar,body_fr,body_ar,image_url,published,published_at) VALUES (COALESCE(:tf,''),COALESCE(:ta,''),COALESCE(:bf,''),COALESCE(:ba,''),NULLIF(:img,''),:pub,CASE WHEN :pub THEN NOW() ELSE NULL END) RETURNING id",newsParams(input),Long.class);return Map.of("id",id);
 }
 @PutMapping("/admin/news/{id}") @PreAuthorize("hasAuthority('SITE_WEB_MODIFIER')") public void updateNews(@PathVariable long id,@RequestBody News input){
  MapSqlParameterSource p=new MapSqlParameterSource(newsParams(input)).addValue("id",id);
  if(db.update("UPDATE website_news SET title_fr=COALESCE(NULLIF(:tf,''),title_fr),title_ar=COALESCE(NULLIF(:ta,''),title_ar),body_fr=COALESCE(NULLIF(:bf,''),body_fr),body_ar=COALESCE(NULLIF(:ba,''),body_ar),image_url=COALESCE(NULLIF(:img,''),image_url),published=:pub,published_at=CASE WHEN :pub THEN COALESCE(published_at,NOW()) ELSE NULL END,updated_at=NOW() WHERE id=:id",p)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
 }
 private Map<String,Object> newsParams(News n){Map<String,Object> p=new HashMap<>();p.put("tf",n.titleFr);p.put("ta",n.titleAr);p.put("bf",n.bodyFr);p.put("ba",n.bodyAr);p.put("img",n.imageUrl);p.put("pub",n.published);return p;}
 @DeleteMapping("/admin/news/{id}") @PreAuthorize("hasAuthority('SITE_WEB_MODIFIER')") public void deleteNews(@PathVariable long id){db.update("DELETE FROM website_news WHERE id=:id",Map.of("id",id));}
 @PostMapping("/admin/gallery") @PreAuthorize("hasAuthority('SITE_WEB_MODIFIER')") public Map<String,Object> addGallery(@RequestBody Gallery g){
  Long id=db.queryForObject("INSERT INTO website_gallery(title_fr,title_ar,image_url,category,published,sort_order) VALUES (COALESCE(:tf,''),COALESCE(:ta,''),COALESCE(:img,''),COALESCE(NULLIF(:cat,''),'general'),:pub,:ord) RETURNING id",galleryParams(g),Long.class);return Map.of("id",id);
 }
 @PutMapping("/admin/gallery/{id}") @PreAuthorize("hasAuthority('SITE_WEB_MODIFIER')") public void updateGallery(@PathVariable long id,@RequestBody Gallery g){
  if(db.update("UPDATE website_gallery SET title_fr=COALESCE(NULLIF(:tf,''),title_fr),title_ar=COALESCE(NULLIF(:ta,''),title_ar),image_url=COALESCE(NULLIF(:img,''),image_url),category=COALESCE(NULLIF(:cat,''),category),published=:pub,sort_order=:ord WHERE id=:id",new MapSqlParameterSource(galleryParams(g)).addValue("id",id))==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
 }
 private Map<String,Object> galleryParams(Gallery g){Map<String,Object> p=new HashMap<>();p.put("tf",g.titleFr);p.put("ta",g.titleAr);p.put("img",g.imageUrl);p.put("cat",g.category);p.put("pub",g.published && g.imageUrl != null && !g.imageUrl.isBlank());p.put("ord",g.sortOrder);return p;}
 @DeleteMapping("/admin/gallery/{id}") @PreAuthorize("hasAuthority('SITE_WEB_MODIFIER')") public void deleteGallery(@PathVariable long id){db.update("DELETE FROM website_gallery WHERE id=:id",Map.of("id",id));}
 @PostMapping(value="/admin/media",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAuthority('SITE_WEB_MODIFIER')") public Map<String,String> upload(@RequestParam("file") MultipartFile file) throws Exception {
  if(file.isEmpty()||file.getSize()>5*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image vide ou supérieure à 5 Mo");
  byte[] data=file.getBytes();String ext;
  if(data.length>3&&(data[0]&255)==255&&(data[1]&255)==216&&(data[2]&255)==255)ext="jpg";
  else if(data.length>8&&(data[0]&255)==137&&data[1]==80&&data[2]==78&&data[3]==71)ext="png";
  else if(data.length>12&&new String(data,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(data,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"))ext="webp";
  else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Formats acceptés : JPEG, PNG, WebP");
  Files.createDirectories(mediaRoot);String name=UUID.randomUUID()+"."+ext;Files.write(mediaRoot.resolve(name),data,StandardOpenOption.CREATE_NEW);
  return Map.of("url","/api/website/public/media/"+name);
 }
 public static class Content {
  @JsonProperty("title_fr") public String titleFr="";
  @JsonProperty("title_ar") public String titleAr="";
  @JsonProperty("body_fr") public String bodyFr="";
  @JsonProperty("body_ar") public String bodyAr="";
  @JsonProperty("image_url") public String imageUrl;
  public boolean published;
 }
 public static class News extends Content {}
 public static class Gallery {
  @JsonProperty("title_fr") public String titleFr="";
  @JsonProperty("title_ar") public String titleAr="";
  @JsonProperty("image_url") public String imageUrl;
  public String category="general";
  public boolean published;
  @JsonProperty("sort_order") public int sortOrder;
 }
}
