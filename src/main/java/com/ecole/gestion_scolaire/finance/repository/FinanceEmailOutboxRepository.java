package com.ecole.gestion_scolaire.finance.repository;
import com.ecole.gestion_scolaire.finance.entity.FinanceEmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FinanceEmailOutboxRepository extends JpaRepository<FinanceEmailOutbox,Long>{
    List<FinanceEmailOutbox> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
