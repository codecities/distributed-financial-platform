package com.dfp.accounts;

import com.dfp.accounts.application.AccountService;
import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.Currency;
import com.dfp.accounts.domain.MovementType;
import com.dfp.accounts.repository.AccountMovementRepository;
import com.dfp.accounts.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
public class AccountCreditTransactionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMovementRepository accountMovementRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldCreditAccountAndRegisterMovementAtomically() {

        UUID accountId = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), Currency.COP)
        ).getId();

        UUID operationId = UUID.randomUUID();

        accountService.creditAccount(
                accountId,
                operationId,
                new BigDecimal("150000.00")
        );

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {

            Account account = accountRepository
                    .findById(accountId)
                    .orElseThrow();

            var movement = accountMovementRepository
                    .findByOperationId(operationId)
                    .orElseThrow();

            assertEquals(
                    0,
                    new BigDecimal("150000.00")
                            .compareTo(account.getBalance())
            );

            assertEquals(accountId, movement.getAccountId());

            assertEquals(MovementType.CREDIT, movement.getType());

            assertEquals(Currency.COP, movement.getCurrency());

            assertEquals(
                    0,
                    account.getBalance().compareTo(movement.getAmount())
            );

            assertEquals(1L, account.getVersion());
        });
    }

    @Test
    void shouldRollbackCreditWhenMovementPersistenceFails() {

        UUID accountId = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), Currency.COP)
        ).getId();

        UUID operationId = UUID.randomUUID();

        // Firsts operation: successfully
        accountService.creditAccount(
                accountId,
                operationId,
                new BigDecimal("100000.00")
        );

        // Second operation: reuse the same operationId
        assertThrows(
                DataIntegrityViolationException.class,
                () -> accountService.creditAccount(
                        accountId,
                        operationId,
                        new BigDecimal("50000.00")
                )
        );

        // Check status saved after exception
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            Account account = accountRepository
                    .findById(accountId)
                    .orElseThrow();

            assertEquals(
                    0,
                    new BigDecimal("100000.00")
                            .compareTo(account.getBalance())
            );

            assertEquals(1L, account.getVersion());

            assertEquals(
                    1L,
                    accountMovementRepository.countByAccountId(accountId)
            );
        });
    }
}
