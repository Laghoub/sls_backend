package com.ecole.gestion_scolaire.canteen;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping("/api/canteen/menus")
public class CanteenMenuController {
 private final JdbcTemplate db;
 public CanteenMenuController(JdbcTemplate db) {this.db=db;}
 public record Day(LocalDate date, String starter, String mainCourse, String sideDish, String dessert, String notes) {}
 public record SaveMenu(LocalDate weekStart, List<Day> days) {}

 private static LocalDate sunday(LocalDate date){return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));}
 private static void validate(SaveMenu m){
  if(m==null || m.weekStart()==null || !m.weekStart().equals(sunday(m.weekStart()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"weekStart doit être un dimanche");
  if(m.days()==null || m.days().isEmpty() || m.days().size()>5) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Entre 1 et 5 jours requis");
  Set<LocalDate> dates=new HashSet<>();
  for(Day d:m.days()){
   if(d==null || d.date()==null || d.date().isBefore(m.weekStart()) || d.date().isAfter(m.weekStart().plusDays(4)) || !dates.add(d.date()) || d.mainCourse()==null || d.mainCourse().isBlank())
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Jour ou plat principal invalide");
  }
 }

 @PutMapping
 @Transactional
 @PreAuthorize("hasAuthority('CANTINE_MENU_GERER')")
 public Map<String,Object> save(@RequestBody SaveMenu m){
  validate(m);
  db.update("INSERT INTO canteen_weekly_menu(week_start) VALUES (?) ON CONFLICT (week_start) DO NOTHING",m.weekStart());
  Map<String,Object> existing=db.queryForMap("SELECT id,status FROM canteen_weekly_menu WHERE week_start=? FOR UPDATE",m.weekStart());
  if("PUBLISHED".equals(existing.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Dépublier avant de modifier le menu");
  long id=((Number)existing.get("id")).longValue();
  db.update("DELETE FROM canteen_menu_day WHERE menu_id=?",id);
  for(Day d:m.days()) db.update("INSERT INTO canteen_menu_day(menu_id,meal_date,starter,main_course,side_dish,dessert,notes) VALUES (?,?,?,?,?,?,?)",id,d.date(),d.starter(),d.mainCourse(),d.sideDish(),d.dessert(),d.notes());
  db.update("UPDATE canteen_weekly_menu SET updated_at=now() WHERE id=?",id);
  return detail(m.weekStart(),false);
 }

 @PutMapping("/{weekStart}/publication")
 @Transactional
 @PreAuthorize("hasAuthority('CANTINE_MENU_PUBLIER')")
 public Map<String,Object> publish(@PathVariable LocalDate weekStart,@RequestParam boolean published){
  int n=db.update("UPDATE canteen_weekly_menu SET status=?,published_at=?,updated_at=now() WHERE week_start=?",published?"PUBLISHED":"DRAFT",published?java.sql.Timestamp.from(Instant.now()):null,weekStart);
  if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Menu introuvable");
  return detail(weekStart,false);
 }

 @GetMapping
 @PreAuthorize("hasAuthority('CANTINE_MENU_CONSULTER')")
 public List<Map<String,Object>> history(){return db.queryForList("SELECT id,week_start,status,published_at,updated_at FROM canteen_weekly_menu ORDER BY week_start DESC");}

 @GetMapping("/{weekStart}")
 @PreAuthorize("hasAuthority('CANTINE_MENU_CONSULTER')")
 public Map<String,Object> detailAdmin(@PathVariable LocalDate weekStart){return detail(weekStart,false);}

 @GetMapping("/current")
 @PreAuthorize("hasAuthority('PORTAIL_PARENT_ACCEDER')")
 public Map<String,Object> current(){return detail(sunday(LocalDate.now(ZoneId.of("Africa/Algiers"))),true);}

 private Map<String,Object> detail(LocalDate weekStart,boolean parent){
  List<Map<String,Object>> menus=db.queryForList("SELECT id,week_start,status,published_at FROM canteen_weekly_menu WHERE week_start=?"+(parent?" AND status='PUBLISHED'":""),weekStart);
  if(menus.isEmpty()) {if(parent)return Map.of("weekStart",weekStart.toString(),"published",false,"days",List.of());throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Menu introuvable");}
  Map<String,Object> menu=menus.get(0);long id=((Number)menu.get("id")).longValue();
  Map<String,Object> result=new LinkedHashMap<>(menu);
  result.put("published","PUBLISHED".equals(menu.get("status")));
  result.put("days",db.queryForList("SELECT meal_date AS date,starter,main_course AS \"mainCourse\",side_dish AS \"sideDish\",dessert,notes FROM canteen_menu_day WHERE menu_id=? ORDER BY meal_date",id));
  return result;
 }
}
