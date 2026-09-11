package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.FamilyCreditUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FamilyCreditUsageRepository extends JpaRepository<FamilyCreditUsage, Long> {
    List<FamilyCreditUsage> findByFamilyCreditIdOrderByCreatedAtDesc(Long familyCreditId);
}
