package com.ecole.gestion_scolaire.audit.config;
import com.ecole.gestion_scolaire.audit.service.AuditService;
import com.ecole.gestion_scolaire.security.service.SecurityUser;
import jakarta.servlet.http.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.UUID;
import java.util.regex.*;

@Component
public class AuditInterceptor implements HandlerInterceptor {
 private static final Logger log=LoggerFactory.getLogger(AuditInterceptor.class);
 private static final String AUDIT_USER_ATTR="audit.securityUser";
 private final AuditService audit; public AuditInterceptor(AuditService audit){this.audit=audit;}
 @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler){
  Authentication a=SecurityContextHolder.getContext().getAuthentication();
  if(a!=null && a.getPrincipal() instanceof SecurityUser su) req.setAttribute(AUDIT_USER_ATTR,su);
  return true;
 }
 @Override public void afterCompletion(HttpServletRequest req,HttpServletResponse res,Object handler,Exception ex){
  String m=req.getMethod(); if(!("POST".equals(m)||"PUT".equals(m)||"PATCH".equals(m)||"DELETE".equals(m))) return;
  String path=req.getRequestURI(); if(path.startsWith("/api/audit")) return;
  SecurityUser u=(SecurityUser)req.getAttribute(AUDIT_USER_ATTR);
  if(u==null){Authentication a=SecurityContextHolder.getContext().getAuthentication(); if(a!=null&&a.getPrincipal() instanceof SecurityUser su)u=su;}
  String module=module(path), action=action(m,path), entityId=lastNumeric(path), entityType=entityType(path);
  String ip=req.getHeader("X-Forwarded-For"); if(ip==null||ip.isBlank()) ip=req.getRemoteAddr(); else ip=ip.split(",")[0].trim();
  String rid=req.getHeader("X-Request-ID"); if(rid==null||rid.isBlank()) rid=UUID.randomUUID().toString();
  String desc=(res.getStatus()>=400?"Échec ":"")+action+" - "+path;
  try{audit.record(u,module,action,entityType,entityId,desc,null,null,m,path,res.getStatus(),ip,rid);}catch(Exception e){log.error("Impossible d’enregistrer l’audit pour {} {}",m,path,e);}
 }
 private String module(String p){String[] a=p.split("/"); if(a.length<3)return "AUTRE";String x=a[2].toUpperCase().replace('-','_');return switch(x){case "TEACHER_ATTENDANCE"->"ASSIDUITE_ENSEIGNANT";case "STUDENT_ATTENDANCE"->"ASSIDUITE_ELEVE";case "AUTH"->"SECURITE";case "ACCESS_MANAGEMENT"->"DROITS_ACCES";default->x;};}
 private String action(String m,String p){String l=p.toLowerCase(); if(l.contains("approve"))return "VALIDATION";if(l.contains("reject"))return "REJET";if(l.contains("cancel"))return "ANNULATION";if(l.contains("refund"))return "REMBOURSEMENT";if(l.contains("login"))return "CONNEXION";if(l.contains("logout"))return "DECONNEXION";if(l.contains("change-password"))return "CHANGEMENT_MOT_DE_PASSE";return switch(m){case "POST"->"CREATION_ACTION";case "PUT","PATCH"->"MODIFICATION";case "DELETE"->"SUPPRESSION";default->m;};}
 private String lastNumeric(String p){Matcher m=Pattern.compile("/(\\d+)(?:/|$)").matcher(p);String v=null;while(m.find())v=m.group(1);return v;}
 private String entityType(String p){String[] a=p.split("/");return a.length>=3?a[a.length>3?a.length-2:2].toUpperCase().replace('-','_'):null;}
}
