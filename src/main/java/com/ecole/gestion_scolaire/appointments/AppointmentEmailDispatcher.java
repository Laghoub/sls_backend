package com.ecole.gestion_scolaire.appointments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AppointmentEmailDispatcher {
 private final JdbcTemplate db; private final ApplicationContext context;
 @Value("${app.appointments.mail.from:${spring.mail.username:}}") private String from;
 @Value("${app.appointments.mail.enabled:true}") private boolean enabled;
 public AppointmentEmailDispatcher(JdbcTemplate db,ApplicationContext context){this.db=db;this.context=context;}
 @Scheduled(initialDelay=8000,fixedDelay=30000)
 public void deliver(){if(!enabled)return;List<Long> ids=db.query("SELECT id FROM parent_teacher_appointment_mail WHERE status IN ('PENDING','FAILED') AND next_attempt_at<=now() AND attempts<12 ORDER BY id LIMIT 30",(r,n)->r.getLong(1));for(Long id:ids)send(id);}
 private void send(long id){int claimed=db.update("UPDATE parent_teacher_appointment_mail SET status='SENDING',attempts=attempts+1 WHERE id=? AND status IN ('PENDING','FAILED')",id);if(claimed==0)return;Map<String,Object> m=db.queryForMap("SELECT recipient_email,subject,body FROM parent_teacher_appointment_mail WHERE id=?",id);try{Class<?> senderClass=Class.forName("org.springframework.mail.javamail.JavaMailSender");Object sender=context.getBean(senderClass);Object message=senderClass.getMethod("createMimeMessage").invoke(sender);Class<?> mimeClass=Class.forName("jakarta.mail.internet.MimeMessage");Class<?> helperClass=Class.forName("org.springframework.mail.javamail.MimeMessageHelper");Object helper=helperClass.getConstructor(mimeClass,boolean.class,String.class).newInstance(message,true,"UTF-8");helperClass.getMethod("setTo",String.class).invoke(helper,m.get("recipient_email").toString());helperClass.getMethod("setSubject",String.class).invoke(helper,m.get("subject").toString());helperClass.getMethod("setText",String.class,boolean.class).invoke(helper,m.get("body").toString(),true);Resource logo=new ClassPathResource("sls-logo.png");if(logo.exists())helperClass.getMethod("addInline",String.class,Resource.class).invoke(helper,"slsLogo",logo);if(from!=null&&!from.isBlank())helperClass.getMethod("setFrom",String.class).invoke(helper,from);senderClass.getMethod("send",mimeClass).invoke(sender,message);db.update("UPDATE parent_teacher_appointment_mail SET status='SENT',sent_at=now(),last_error=NULL WHERE id=?",id);}catch(Exception ex){String err=String.valueOf(ex.getCause()!=null?ex.getCause():ex);db.update("UPDATE parent_teacher_appointment_mail SET status='FAILED',last_error=?,next_attempt_at=now()+ interval '5 minutes' WHERE id=?",err.substring(0,Math.min(err.length(),1800)),id);}}
 @Scheduled(initialDelay=60000,fixedDelay=300000)
 public void recover(){db.update("UPDATE parent_teacher_appointment_mail SET status='FAILED',next_attempt_at=now() WHERE status='SENDING' AND created_at<now()-interval '10 minutes'");}
}
