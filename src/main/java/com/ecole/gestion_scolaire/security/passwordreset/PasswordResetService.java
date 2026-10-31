package com.ecole.gestion_scolaire.security.passwordreset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class PasswordResetService {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final ApplicationContext context;
    private final SecureRandom random=new SecureRandom();
    @Value("${app.frontend-url:http://localhost:4200}") private String frontendUrl;
    @Value("${app.finance-email.from:}") private String from;
    @Value("${app.finance-email.from-name:Groupe Scolaire Sciences et Lettres}") private String fromName;

    public PasswordResetService(JdbcTemplate db,PasswordEncoder encoder,ApplicationContext context) {
        this.db=db;this.encoder=encoder;this.context=context;
    }

    // Réponse toujours identique, même si l'identifiant n'existe pas.
    // Limitation à 3 demandes par heure et par compte, plus 60 secondes entre demandes.
    public void request(String username) {
        if(username==null||username.isBlank()||username.length()>100)return;
        List<Map<String,Object>> users=db.queryForList(
            "SELECT u.id,p.email,p.first_name FROM user_account u JOIN person p ON p.id=u.person_id "+
            "WHERE lower(u.username)=lower(?) AND u.status='ACTIVE' AND p.email IS NOT NULL AND trim(p.email)<>''",
            username.trim());
        if(users.isEmpty())return;
        Map<String,Object> u=users.get(0);
        long id=((Number)u.get("id")).longValue();
        Long recent=db.queryForObject(
            "SELECT count(*) FROM password_reset_token WHERE user_account_id=? AND created_at>now()-interval '1 hour'",
            Long.class,id);
        Long cooldown=db.queryForObject(
            "SELECT count(*) FROM password_reset_token WHERE user_account_id=? AND created_at>now()-interval '60 seconds'",
            Long.class,id);
        if(recent!=null&&recent>=3||cooldown!=null&&cooldown>0)return;
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash=hash(token);
        db.update("UPDATE password_reset_token SET used_at=now() WHERE user_account_id=? AND used_at IS NULL",id);
        db.update("INSERT INTO password_reset_token(user_account_id,token_hash,expires_at) VALUES(?,?,now()+interval '30 minutes')",id,hash);
        String link=frontendUrl.replaceAll("/+$","")+"/reset-password?token="+token;
        // Si SMTP est indisponible, ne pas exposer l'existence du compte.
        send(String.valueOf(u.get("email")),String.valueOf(u.get("first_name")),link);
    }

    @Transactional
    public void confirm(String token,String password,String confirmation) {
        if(token==null||token.length()<32||token.length()>200)
            throw invalid();
        if(password==null||password.length()<10||password.length()>128||!password.equals(confirmation))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mot de passe invalide ou confirmation différente (10 caractères minimum).");
        String hash=hash(token);
        List<Map<String,Object>> rows=db.queryForList(
            "SELECT t.id,t.user_account_id,t.expires_at,t.used_at,u.status FROM password_reset_token t "+
            "JOIN user_account u ON u.id=t.user_account_id WHERE t.token_hash=? FOR UPDATE OF t,u",hash);
        if(rows.isEmpty())throw invalid();
        Map<String,Object> row=rows.get(0);
        // JdbcTemplate peut retourner java.sql.Timestamp pour TIMESTAMPTZ.
        // Comparer les instants en UTC évite un cast invalide et les erreurs de fuseau.
        Object expiryValue=row.get("expires_at");
        java.time.Instant expires;
        if(expiryValue instanceof java.sql.Timestamp ts) {
            expires=ts.toInstant();
        } else if(expiryValue instanceof OffsetDateTime odt) {
            expires=odt.toInstant();
        } else if(expiryValue instanceof java.time.Instant instant) {
            expires=instant;
        } else {
            throw new IllegalStateException("Type SQL inattendu pour expires_at: " +
                (expiryValue==null ? "null" : expiryValue.getClass().getName()));
        }
        if(row.get("used_at")!=null||!expires.isAfter(java.time.Instant.now())||!"ACTIVE".equals(row.get("status")))
            throw invalid();
        long userId=((Number)row.get("user_account_id")).longValue();
        db.update("UPDATE user_account SET password_hash=?,password_changed_at=now(),must_change_password=false,"+
            "failed_login_attempts=0,locked_until=NULL,updated_at=now() WHERE id=?",encoder.encode(password),userId);
        db.update("UPDATE password_reset_token SET used_at=now() WHERE user_account_id=? AND used_at IS NULL",userId);
    }

    private ResponseStatusException invalid(){
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ce lien est invalide, expiré ou déjà utilisé.");
    }

    private static String hash(String token){
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static String escape(String v){
        return v==null?"":v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
    private void send(String email,String firstName,String link){
        try {
            Class<?> senderClass=Class.forName("org.springframework.mail.javamail.JavaMailSender");
            Object sender=context.getBean(senderClass);
            Object message=senderClass.getMethod("createMimeMessage").invoke(sender);
            Class<?> mime=Class.forName("jakarta.mail.internet.MimeMessage");
            Class<?> helper=Class.forName("org.springframework.mail.javamail.MimeMessageHelper");
            Object h=helper.getConstructor(mime,boolean.class,String.class).newInstance(message,true,"UTF-8");
            helper.getMethod("setTo",String.class).invoke(h,email);
            helper.getMethod("setSubject",String.class).invoke(h,"Réinitialisation de votre mot de passe — Sciences et Lettres");
            String html="<div style='background:#f1f5f9;padding:32px;font-family:Arial,sans-serif'>"+
              "<div style='max-width:580px;margin:auto;background:white;border-radius:12px;overflow:hidden'>"+
              "<div style='background:#102b49;padding:25px;text-align:center;color:white'>"+
              "<img src='cid:slsLogo' alt='' width='72' style='display:block;margin:0 auto 12px'>"+
              "<h2 style='margin:0'>Groupe Scolaire Sciences et Lettres</h2>"+
              "<div>المجمع المدرسي الخاص علوم وآداب</div></div>"+
              "<div style='padding:28px;color:#233449'><p>Bonjour "+escape(firstName)+",</p>"+
              "<p>Une demande de réinitialisation de mot de passe a été reçue pour votre compte scolaire.</p>"+
              "<p style='text-align:center;margin:32px 0'><a href='"+escape(link)+"' "+
              "style='background:#1c5c9a;color:white;text-decoration:none;padding:14px 24px;border-radius:7px'>"+
              "Réinitialiser mon mot de passe</a></p>"+
              "<p>Ce lien est valable <strong>30 minutes</strong> et ne peut être utilisé qu'une seule fois.</p>"+
              "<p>Si vous n'avez pas demandé cette modification, ignorez cet e-mail. Votre mot de passe reste inchangé.</p>"+
              "</div></div></div>";
            helper.getMethod("setText",String.class,boolean.class).invoke(h,html,true);
            if(from!=null&&!from.isBlank())helper.getMethod("setFrom",String.class,String.class).invoke(h,from,fromName);
            Resource logo=new ClassPathResource("sls-logo.png");
            if(logo.exists())helper.getMethod("addInline",String.class,Resource.class).invoke(h,"slsLogo",logo);
            senderClass.getMethod("send",mime).invoke(sender,message);
        } catch(Exception ex){
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Échec de l'envoi du lien de réinitialisation",ex);
        }
    }
}
