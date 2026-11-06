package com.dfp.accounts;

import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.Currency;
import com.dfp.accounts.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
public class AccountCreditPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldPersistAccountBalanceAfterCredit() {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        // Given: cuenta persistida
        UUID accountId = transaction.execute(status -> {
            Account account = new Account(
                    UUID.randomUUID(),
                    Currency.COP
            );

            return accountRepository.saveAndFlush(account).getId();
        });

        // When: acreditar dentro de una transaccion
        transaction.executeWithoutResult(transactionStatus -> {
            Account account = accountRepository
                    .findById(accountId)
                    .orElseThrow();

            account.credit(new BigDecimal("150000.00"));
        });

        // Then: comprobar los datos persistidos
        transaction.executeWithoutResult(transactionStatus -> {

            entityManager.clear();

            Account persistedAccount = accountRepository
                    .findById(accountId)
                    .orElseThrow();

            assertEquals(
                    0,
                    new BigDecimal("150000.00")
                            .compareTo(persistedAccount.getBalance())
            );

            assertEquals(
                    1L,
                    persistedAccount.getVersion()
            );
        });
    }
}
