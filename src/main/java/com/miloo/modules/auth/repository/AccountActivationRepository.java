package com.miloo.modules.auth.repository;

import com.miloo.modules.auth.entity.AccountActivationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountActivationRepository extends JpaRepository<AccountActivationEntity, UUID> {

    Optional<AccountActivationEntity> findByToken(String token);

    Optional<AccountActivationEntity> findByTokenAndUsedAtIsNull(String token);
}
