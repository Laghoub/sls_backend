package com.ecole.gestion_scolaire.attendance.repository;
import com.ecole.gestion_scolaire.attendance.entity.AttendanceEmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AttendanceEmailOutboxRepository extends JpaRepository<AttendanceEmailOutbox,Long> {
    List<AttendanceEmailOutbox> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
