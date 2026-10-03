package com.ecole.gestion_scolaire.accountactivation.repository;
import com.ecole.gestion_scolaire.accountactivation.entity.AccountActivationToken; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken,Long>{ Optional<AccountActivationToken> findByTokenHash(String tokenHash); }
