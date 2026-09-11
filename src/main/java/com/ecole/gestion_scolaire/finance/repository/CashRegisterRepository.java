package com.ecole.gestion_scolaire.finance.repository;

import com.ecole.gestion_scolaire.finance.entity.CashRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {
    Optional<CashRegister> findByCodeIgnoreCase(String code);

    List<CashRegister> findByActiveTrueOrderByNameAsc();
}
