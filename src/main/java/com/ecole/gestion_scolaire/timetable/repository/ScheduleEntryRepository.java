package com.ecole.gestion_scolaire.timetable.repository;

import com.ecole.gestion_scolaire.timetable.entity.ScheduleEntry;
import com.ecole.gestion_scolaire.timetable.enums.ScheduleEntryStatus;
import com.ecole.gestion_scolaire.timetable.enums.SchoolDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

    @Query("""
        select e from ScheduleEntry e
        join fetch e.teachingAssignment a
        join fetch a.teacher t
        join fetch t.employee emp
        join fetch emp.person p
        join fetch a.subject s
        join fetch a.classGroup c
        join fetch a.schoolYear y
        join fetch e.timeSlot ts
        left join fetch e.room r
        where a.schoolYear.id = :schoolYearId
          and a.classGroup.id = :classGroupId
          and e.status = :status
        order by e.dayOfWeek, ts.displayOrder
    """)
    List<ScheduleEntry> findClassSchedule(Long schoolYearId, Long classGroupId, ScheduleEntryStatus status);

    @Query("""
        select e from ScheduleEntry e
        join fetch e.teachingAssignment a
        join fetch a.teacher t
        join fetch t.employee emp
        join fetch emp.person p
        join fetch a.subject s
        join fetch a.classGroup c
        join fetch a.schoolYear y
        join fetch e.timeSlot ts
        left join fetch e.room r
        where a.schoolYear.id = :schoolYearId
          and a.teacher.id = :teacherId
          and e.status = :status
        order by e.dayOfWeek, ts.displayOrder
    """)
    List<ScheduleEntry> findTeacherSchedule(Long schoolYearId, Long teacherId, ScheduleEntryStatus status);

    @Query("""
        select e from ScheduleEntry e
        join fetch e.teachingAssignment a
        join fetch a.teacher t
        join fetch t.employee emp
        join fetch emp.person p
        join fetch a.subject s
        join fetch a.classGroup c
        join fetch a.schoolYear y
        join fetch e.timeSlot ts
        left join fetch e.room r
        where a.schoolYear.id = :schoolYearId
          and e.dayOfWeek = :day
          and e.status = :status
    """)
    List<ScheduleEntry> findActiveForConflictCheck(Long schoolYearId, SchoolDay day, ScheduleEntryStatus status);

    @Query("""
        select e from ScheduleEntry e
        join fetch e.teachingAssignment a
        join fetch a.teacher t
        join fetch t.employee emp
        join fetch emp.person p
        join fetch a.subject s
        join fetch a.classGroup c
        join fetch a.schoolYear y
        join fetch e.timeSlot ts
        left join fetch e.room r
        where e.dayOfWeek = :day
          and e.timeSlot.id = :timeSlotId
          and e.status = :status
          and e.validFrom <= :date
          and (e.validUntil is null or e.validUntil >= :date)
          and a.startDate <= :date
          and (a.endDate is null or a.endDate >= :date)
        order by p.lastName, p.firstName
    """)
    List<ScheduleEntry> findExpectedForAttendance(
            SchoolDay day, Long timeSlotId, java.time.LocalDate date, ScheduleEntryStatus status);

    List<ScheduleEntry> findByTeachingAssignmentClassGroupIdAndTeachingAssignmentSchoolYearIdAndStatus(
            Long classGroupId, Long schoolYearId, ScheduleEntryStatus status);
}
