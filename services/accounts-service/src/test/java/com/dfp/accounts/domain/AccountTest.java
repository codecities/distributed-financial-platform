package com.dfp.accounts.domain;

import com.dfp.accounts.domain.exception.InvalidAccountStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    @Test
    void shouldCreateAnActiveAccount() {
        // Given
        UUID customerId = UUID.randomUUID();
        Instant beforeCreation = Instant.now();

        // When
        Account account = new Account(customerId, Currency.COP);

        Instant afterCreation = Instant.now();

        // Then
        assertAll(
                () -> assertNotNull(account.getId()),
                () -> assertEquals(customerId, account.getCustomerId()),
                () -> assertEquals(Currency.COP, account.getCurrency()),
                () -> assertEquals(AccountStatus.ACTIVE, account.getStatus()),
                () -> assertFalse(
                        account.getCreatedAt().isBefore(beforeCreation)
                ),
                () -> assertFalse(
                        account.getCreatedAt().isAfter(afterCreation)
                )
        );
    }

    @Test
    void shouldRejectNullCustomerId() {
        assertThrows(
                NullPointerException.class,
                () -> new Account(null, Currency.COP)
        );
    }

    @Test
    void shouldRejectNullCurrency() {
        assertThrows(
                NullPointerException.class,
                () -> new Account(UUID.randomUUID(), null)
        );
    }

    @Test
    void shouldGenerateDifferentIdsForDifferentAccounts() {
        UUID customerId = UUID.randomUUID();

        Account account1 = new Account(customerId, Currency.COP);
        Account account2 = new Account(customerId, Currency.COP);

        assertNotEquals(account1.getId(), account2.getId());
    }

    @Test
    void shouldCreateAccountsWithDifferentCurrencies() {
        UUID customerId = UUID.randomUUID();

        Account account1 = new Account(customerId, Currency.COP);
        Account account2 = new Account(customerId, Currency.USD);

        assertAll(
                () -> assertEquals(Currency.COP, account1.getCurrency()),
                () -> assertEquals(Currency.USD, account2.getCurrency()),
                () -> assertEquals(customerId, account1.getCustomerId()),
                () -> assertEquals(customerId, account2.getCustomerId()),
                () -> assertNotEquals(account1.getId(), account2.getId())
        );
        assertNotEquals(account1.getId(), account2.getId());
    }

    @Test
    void shouldSuspendActiveAccount() {
        // Given
        Account account = new Account(
                UUID.randomUUID(),
                Currency.COP
        );

        account.suspend();

        assertThrows(
                InvalidAccountStateException.class,
                account::suspend
        );

        assertEquals(
                AccountStatus.SUSPENDED,
                account.getStatus()
        );
    }
}
