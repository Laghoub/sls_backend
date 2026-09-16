package com.ecole.gestion_scolaire.attendance.service;

import com.ecole.gestion_scolaire.attendance.entity.*;
import com.ecole.gestion_scolaire.student.repository.StudentGuardianRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AttendanceEmailService {
    private static final String LOGO_CID="slsLogo";
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH'h'mm");
    private final com.ecole.gestion_scolaire.attendance.repository.AttendanceEmailOutboxRepository outbox;
    private final StudentGuardianRepository studentGuardians;
    private final ApplicationContext context;
    @Value("${app.attendance-email.enabled:true}") private boolean enabled;
    @Value("${app.finance-email.from:sls.school2008@gmail.com}") private String from;
    @Value("${app.finance-email.from-name:Groupe Scolaire Sciences et Lettres}") private String fromName;
    public AttendanceEmailService(com.ecole.gestion_scolaire.attendance.repository.AttendanceEmailOutboxRepository o, StudentGuardianRepository sg, ApplicationContext c){outbox=o;studentGuardians=sg;context=c;}

    public void teacherAbsent(TeacherAttendance a){
        var p=a.getTeacher().getEmployee().getPerson();
        if(blank(p.getEmail())) return;
        var s=a.getClassSession();
        String body=layout("Absence enregistrée", "Bonjour " + esc(p.getFirstName()) + ",",
                "Une absence a été enregistrée à votre nom.<br><br>"+attendanceDetails(s)+"<br><br>Merci de transmettre votre justificatif d’absence à l’administration dans les meilleurs délais.");
        queue(p.getEmail(), "Absence à justifier - " + DATE.format(s.getSessionDate()), body, "TEACHER_ABSENT", a.getId());
    }

    public void studentIncident(StudentAttendance a){
        var student=a.getStudent(); var p=student.getPerson(); var s=a.getClassSession();
        String incident=a.getAttendanceStatus().name().equals("ABSENT") ? "une absence" : "un retard de " + a.getLateMinutes() + " minute(s)";
        String title=a.getAttendanceStatus().name().equals("ABSENT") ? "Absence de votre enfant" : "Retard de votre enfant";
        Set<String> emails=new LinkedHashSet<>(); LocalDate date=s.getSessionDate();
        for(var link:studentGuardians.findByStudentIdOrderByIdAsc(student.getId())){
            if(!link.isActive()) continue;
            if(link.getValidFrom()!=null && date.isBefore(link.getValidFrom())) continue;
            if(link.getValidUntil()!=null && date.isAfter(link.getValidUntil())) continue;
            var gp=link.getGuardian().getPerson(); if(!blank(gp.getEmail())) emails.add(gp.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        for(String email:emails){
            String body=layout(title, "Bonjour,", "Nous vous informons que <b>"+esc(p.getFirstName()+" "+p.getLastName())+"</b> a enregistré <b>"+incident+"</b>.<br><br>"+attendanceDetails(s)+"<br><br>En cas d’absence, merci de transmettre un justificatif à l’établissement. Pour un retard, ce message vous informe du retard constaté.");
            queue(email, title + " - " + p.getFirstName()+" "+p.getLastName(), body, a.getAttendanceStatus().name().equals("ABSENT")?"STUDENT_ABSENT":"STUDENT_LATE", a.getId());
        }
    }
    private void queue(String to,String subject,String body,String type,Long attendanceId){var e=new AttendanceEmailOutbox();e.setRecipientEmail(to.trim());e.setSubject(subject+" - Groupe Scolaire Sciences et Lettres");e.setBodyHtml(body);e.setNotificationType(type);e.setAttendanceId(attendanceId);e=outbox.save(e);schedule(e.getId());}
    private void schedule(Long id){if(!enabled)return;if(TransactionSynchronizationManager.isActualTransactionActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){tryDeliver(id);}});else tryDeliver(id);}
    @Scheduled(initialDelayString="${app.attendance-email.dispatcher.initial-delay-ms:5000}",fixedDelayString="${app.attendance-email.dispatcher.fixed-delay-ms:30000}") public void retryPending(){if(!enabled)return;for(var e:outbox.findTop50ByStatusOrderByCreatedAtAsc("PENDING"))tryDeliver(e.getId());}
    public synchronized boolean tryDeliver(Long id){if(!enabled)return false;var e=outbox.findById(id).orElse(null);if(e==null||"SENT".equals(e.getStatus()))return false;try{Class<?> sc=Class.forName("org.springframework.mail.javamail.JavaMailSender");Object sender=context.getBean(sc);Object msg=sc.getMethod("createMimeMessage").invoke(sender);Class<?> mc=Class.forName("jakarta.mail.internet.MimeMessage");Class<?> hc=Class.forName("org.springframework.mail.javamail.MimeMessageHelper");Object h=hc.getConstructor(mc,boolean.class,String.class).newInstance(msg,true,"UTF-8");hc.getMethod("setTo",String.class).invoke(h,e.getRecipientEmail());hc.getMethod("setSubject",String.class).invoke(h,e.getSubject());hc.getMethod("setText",String.class,boolean.class).invoke(h,e.getBodyHtml(),true);hc.getMethod("setFrom",String.class,String.class).invoke(h,from.trim(),fromName.trim());Resource logo=new ClassPathResource("sls-logo.png");if(logo.exists())hc.getMethod("addInline",String.class,Resource.class).invoke(h,LOGO_CID,logo);sc.getMethod("send",mc).invoke(sender,msg);e.setStatus("SENT");e.setSentAt(OffsetDateTime.now());e.setLastError(null);e.setAttemptCount(e.getAttemptCount()+1);outbox.save(e);return true;}catch(Exception ex){e.setStatus("PENDING");e.setAttemptCount(e.getAttemptCount()+1);String m=ex.getMessage();if(m==null&&ex.getCause()!=null)m=ex.getCause().getMessage();e.setLastError(m==null?ex.getClass().getSimpleName():m.substring(0,Math.min(1800,m.length())));outbox.save(e);return false;}}
    private String attendanceDetails(ClassSession s){
        String horaire = TIME.format(s.getTimeSlot().getStartTime()) + " – " + TIME.format(s.getTimeSlot().getEndTime());
        return "<div style=\'background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px;line-height:1.9\'>"
                + "<b>Date :</b> " + DATE.format(s.getSessionDate()) + "<br>"
                + "<b>Horaire :</b> " + esc(horaire) + "<br>"
                + "<b>Classe :</b> " + esc(s.getClassGroup().getName()) + "<br>"
                + "<b>Matière :</b> " + esc(s.getSubject().getName())
                + "</div>";
    }
    private String layout(String title,String hello,String text){return "<!doctype html><html><body style='margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033'><div style='max-width:680px;margin:24px auto;background:#fff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden'><div style='background:#0f2747;padding:22px;text-align:center'><img src='cid:"+LOGO_CID+"' style='height:70px'><div style='color:#fff;font-size:20px;font-weight:700'>Groupe Scolaire Sciences et Lettres</div></div><div style='padding:28px'><h2 style='color:#0f2747'>"+esc(title)+"</h2><p>"+hello+"</p><p style='line-height:1.7'>"+text+"</p><p style='margin-top:28px;color:#64748b'>Message automatique envoyé par le système de gestion scolaire.</p></div></div></body></html>";}
    private boolean blank(String s){return s==null||s.isBlank();} private String esc(String s){if(s==null)return "";return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
