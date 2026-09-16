package com.ecole.gestion_scolaire.attendance.repository;

import com.ecole.gestion_scolaire.attendance.entity.TeacherAttendance;
import com.ecole.gestion_scolaire.attendance.enums.AttendanceValidationStatus;
import com.ecole.gestion_scolaire.attendance.enums.TeacherAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long>, JpaSpecificationExecutor<TeacherAttendance> {
    Optional<TeacherAttendance> findByClassSessionIdAndTeacherId(Long classSessionId, Long teacherId);
    List<TeacherAttendance> findByClassSessionIdIn(List<Long> sessionIds);
    long countByAttendanceStatus(TeacherAttendanceStatus status);
    long countByValidationStatus(AttendanceValidationStatus status);
}
