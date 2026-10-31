package com.dfp.accounts;

import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.AccountStatus;
import com.dfp.accounts.domain.Currency;
import com.dfp.accounts.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.hibernate.StaleStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class AccountConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldRejectConcurrentAccountModification() {
        // Given: create an account
        UUID customerId = UUID.randomUUID();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        UUID accountId = transaction.execute(status -> {
            Account account = new Account(customerId, Currency.COP);

            return accountRepository.saveAndFlush(account).getId();
        });

        EntityManager emA = entityManagerFactory.createEntityManager();
        EntityManager emB = entityManagerFactory.createEntityManager();

        try {
            // Both contexts read the same initial version
            Account accountA = emA.find(Account.class, accountId);
            Account accountB = emB.find(Account.class, accountId);

            assertEquals(accountA.getVersion(), accountB.getVersion());

            // Transaction A
            emA.getTransaction().begin();

            accountA.suspend();

            emA.getTransaction().commit();

            // Transaction B
            emB.getTransaction().begin();

            accountB.suspend();

            RollbackException exception = assertThrows(
                    RollbackException.class,
                    () -> emB.getTransaction().commit()
            );

            Throwable cause = exception;

            while (cause != null &&
                    !(cause instanceof StaleStateException)) {
                cause = cause.getCause();
            }

            assertInstanceOf(StaleStateException.class, cause);

        } finally {
            if (emA.getTransaction().isActive()) {
                emA.getTransaction().rollback();
            }

            if (emB.getTransaction().isActive()) {
                emB.getTransaction().rollback();
            }

            emA.close();
            emB.close();
        }

        transaction.executeWithoutResult(status -> {

            Account persistedAccount = accountRepository
                    .findById(accountId)
                    .orElseThrow();

            assertEquals(
                    AccountStatus.SUSPENDED,
                    persistedAccount.getStatus()
            );

            assertEquals(
                    1L,
                    persistedAccount.getVersion()
            );
        });
    }
}
