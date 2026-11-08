package com.dfp.accounts.repository;

import com.dfp.accounts.domain.AccountMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountMovementRepository
        extends JpaRepository<AccountMovement, UUID> {

    long countByAccountId(UUID accountId);

    Optional<AccountMovement> findByOperationId(UUID operationId);
}
