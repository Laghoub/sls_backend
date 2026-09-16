package com.ecole.gestion_scolaire.attendance.repository;

import com.ecole.gestion_scolaire.attendance.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    Optional<ClassSession> findByScheduleEntryIdAndSessionDate(Long scheduleEntryId, LocalDate sessionDate);

    @Query("""
        select s from ClassSession s
        join fetch s.teacher t
        join fetch t.employee e
        join fetch e.person p
        join fetch s.classGroup c
        join fetch s.subject sub
        join fetch s.timeSlot ts
        left join fetch s.room r
        where s.sessionDate = :date and s.timeSlot.id = :timeSlotId
        order by p.lastName, p.firstName
    """)
    List<ClassSession> findByDateAndTimeSlot(Long timeSlotId, LocalDate date);
}
