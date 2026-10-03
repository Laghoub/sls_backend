package com.ecole.gestion_scolaire.identity.repository;
import com.ecole.gestion_scolaire.identity.entity.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride,Long>{
 List<UserPermissionOverride> findByUserAccountId(Long userAccountId);
 void deleteByUserAccountId(Long userAccountId);
}
