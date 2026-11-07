package com.dfp.accounts;

import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.AccountMovement;
import com.dfp.accounts.domain.Currency;
import com.dfp.accounts.domain.MovementType;
import com.dfp.accounts.repository.AccountMovementRepository;
import com.dfp.accounts.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
public class AccountMovementPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMovementRepository movementRepository;

    @Test
    void shouldPersistCreditMovement() {
        Account account = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), Currency.COP)
        );

        UUID operationId = UUID.randomUUID();

        AccountMovement movement = AccountMovement.credit(
                account.getId(),
                operationId,
                new BigDecimal("150000.00"),
                Currency.COP
        );

        movementRepository.saveAndFlush(movement);

        AccountMovement persistedMovement =
                movementRepository.findByOperationId(operationId)
                        .orElseThrow();

        assertEquals(movement.getId(), persistedMovement.getId());

        assertEquals(
                0,
                new BigDecimal("150000.00")
                        .compareTo(persistedMovement.getAmount())
        );

        assertEquals(
                MovementType.CREDIT,
                persistedMovement.getType()
        );
    }
}
