package com.ecole.gestion_scolaire.registration.service;
import com.ecole.gestion_scolaire.registration.exception.RegistrationValidationException; import jakarta.persistence.EntityManager; import org.springframework.stereotype.Component;
@Component public class RegistrationReferenceValidator { private final EntityManager em; public RegistrationReferenceValidator(EntityManager em){this.em=em;}
 public void schoolYear(Long id){exists("school_year",id,"Année scolaire");} public void level(Long id){exists("level",id,"Niveau");} public void classGroup(Long id){if(id!=null)exists("class_group",id,"Classe");}
 public void classMatches(Long classId,Long yearId,Long levelId){if(classId==null)return; Number n=(Number)em.createNativeQuery("select count(*) from class_group where id=:c and school_year_id=:y and level_id=:l").setParameter("c",classId).setParameter("y",yearId).setParameter("l",levelId).getSingleResult(); if(n.longValue()==0)throw new RegistrationValidationException("La classe demandée ne correspond pas à l’année scolaire et au niveau choisis.");}
 private void exists(String table,Long id,String label){Number n=(Number)em.createNativeQuery("select count(*) from "+table+" where id=:id").setParameter("id",id).getSingleResult(); if(n.longValue()==0)throw new RegistrationValidationException(label+" introuvable : "+id);}
}
