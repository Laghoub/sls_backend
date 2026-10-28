package com.ecole.gestion_scolaire.attendance.monthly;
import com.ecole.gestion_scolaire.attendance.entity.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface MonthlyPointageRepository extends JpaRepository<TeacherAttendance, Long> {
 @Query("""
 SELECT a FROM TeacherAttendance a
 JOIN FETCH a.classSession s
 JOIN FETCH s.teacher t JOIN FETCH t.employee emp JOIN FETCH emp.person
 JOIN FETCH s.classGroup cg JOIN FETCH cg.level lvl JOIN FETCH lvl.cycle
 JOIN FETCH cg.schoolYear
 JOIN FETCH s.subject JOIN FETCH s.timeSlot
 WHERE s.sessionDate BETWEEN :from AND :to
 AND (:schoolYearId IS NULL OR cg.schoolYear.id = :schoolYearId)
 AND (:cycle IS NULL OR lvl.cycle.code = :cycle)
 AND (:classId IS NULL OR cg.id = :classId)
 AND (:teacherId IS NULL OR t.id = :teacherId)
 AND (:subjectId IS NULL OR s.subject.id = :subjectId)
 ORDER BY emp.person.lastName, emp.person.firstName, cg.name, s.sessionDate
 """)
 List<TeacherAttendance> rows(@Param("from") LocalDate from, @Param("to") LocalDate to,
 @Param("schoolYearId") Long schoolYearId, @Param("cycle") String cycle,
 @Param("classId") Long classId, @Param("teacherId") Long teacherId, @Param("subjectId") Long subjectId);
}
