package com.dfp.accounts;

import com.dfp.accounts.application.AccountService;
import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.Currency;
import com.dfp.accounts.domain.MovementType;
import com.dfp.accounts.domain.exception.DuplicateOperationException;
import com.dfp.accounts.domain.exception.InvalidAccountStateException;
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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

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
                DuplicateOperationException.class,
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

    @Test
    void shouldNotApplySameCreditOperationTwice() {
        UUID accountId = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), Currency.COP)
        ).getId();

        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150000.00");

        // first request
        accountService.creditAccount(
                accountId,
                operationId,
                amount
        );

        // retry same request
        assertDoesNotThrow(() -> {
            accountService.creditAccount(
                    accountId,
                    operationId,
                    amount
            );
        });

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            Account account = accountRepository
                    .findById(accountId)
                    .orElseThrow();

            assertEquals(
                    0,
                    amount.compareTo(account.getBalance())
            );

            assertEquals(
                    1L,
                    accountMovementRepository.countByAccountId(accountId)
            );
        });
    }

    @Test
    void shouldRejectSameOperationIdWithDifferentAmount() {

        UUID accountId = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), Currency.COP)
        ).getId();

        UUID operationId = UUID.randomUUID();

        accountService.creditAccount(
                accountId,
                operationId,
                new BigDecimal("100000.00")
        );

        assertThrows(
                DuplicateOperationException.class,
                () -> accountService.creditAccount(
                        accountId,
                        operationId,
                        new BigDecimal("200000.00")
                )
        );

        Account account = accountRepository
                .findById(accountId)
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("100000.00")
                        .compareTo(account.getBalance())
        );

        assertEquals(
                1L,
                accountMovementRepository.countByAccountId(accountId)
        );
    }

    @Test
    void shouldRejectSameOperationIdWithDifferentAccount() {

        UUID firstAccountId = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), Currency.COP)
        ).getId();

        UUID secondAccountId = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), Currency.COP)
        ).getId();

        UUID operationId = UUID.randomUUID();

        accountService.creditAccount(
                firstAccountId,
                operationId,
                new BigDecimal("100000.00")
        );

        assertThrows(
                DuplicateOperationException.class,
                () -> accountService.creditAccount(
                        secondAccountId,
                        operationId,
                        new BigDecimal("100000.00")
                )
        );

        Account secondAccount = accountRepository
                .findById(secondAccountId)
                        .orElseThrow();

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(secondAccount.getBalance())
        );

        assertEquals(
                0L,
                accountMovementRepository.countByAccountId(secondAccountId)
        );
    }

    @Test
    void shouldNotDuplicateCreditUnderConcurrency() throws Exception {

        UUID accountId = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), Currency.COP)
        ).getId();

        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100000.00");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Boolean> creditTask = () -> {
                start.await();

                try {
                    accountService.creditAccount(
                            accountId,
                            operationId,
                            amount
                    );
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            };

            Future<Boolean> first = executor.submit(creditTask);
            Future<Boolean> second = executor.submit(creditTask);

            start.countDown();

            boolean firstSucceeded = first.get(15, TimeUnit.SECONDS);
            boolean secondSucceded = second.get(15, TimeUnit.SECONDS);

            assertTrue(firstSucceeded || secondSucceded);

            TransactionTemplate transaction =
                    new TransactionTemplate(transactionManager);

            transaction.executeWithoutResult(status -> {
                Account account = accountRepository
                        .findById(accountId)
                        .orElseThrow();

                assertEquals(0, amount.compareTo(account.getBalance()));

                assertEquals(
                        1L,
                        accountMovementRepository.countByAccountId(accountId)
                );

                assertEquals(1L, account.getVersion());
            });

        } finally {
            executor.shutdownNow();
        }
    }
}
