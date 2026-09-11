package com.ecole.gestion_scolaire.student.repository;
import com.ecole.gestion_scolaire.student.entity.AuthorizedPickupPerson; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AuthorizedPickupPersonRepository extends JpaRepository<AuthorizedPickupPerson,Long>{ List<AuthorizedPickupPerson> findByStudentIdOrderByLastNameAscFirstNameAsc(Long studentId); }
