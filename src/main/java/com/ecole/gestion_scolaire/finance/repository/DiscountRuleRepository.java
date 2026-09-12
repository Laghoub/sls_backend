package com.ecole.gestion_scolaire.finance.repository;
import com.ecole.gestion_scolaire.finance.entity.DiscountRule; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface DiscountRuleRepository extends JpaRepository<DiscountRule,Long>{ List<DiscountRule> findBySchoolYearIdOrderByPriorityDescIdAsc(Long schoolYearId); List<DiscountRule> findBySchoolYearIdAndActiveTrueOrderByPriorityDescIdAsc(Long schoolYearId); boolean existsByCodeIgnoreCase(String code); }
