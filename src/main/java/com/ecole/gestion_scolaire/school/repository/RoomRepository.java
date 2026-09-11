package com.ecole.gestion_scolaire.school.repository;

import com.ecole.gestion_scolaire.school.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository
        extends JpaRepository<Room, Long> {

    Optional<Room> findByCampusIdAndCode(
            Long campusId,
            String code
    );

    boolean existsByCampusIdAndCode(
            Long campusId,
            String code
    );

    List<Room> findByCampusIdOrderByNameAsc(
            Long campusId
    );

    List<Room> findByCampusIdAndActiveTrueOrderByNameAsc(
            Long campusId
    );
}