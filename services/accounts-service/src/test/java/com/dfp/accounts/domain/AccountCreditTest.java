package com.dfp.accounts.domain;

import com.dfp.accounts.domain.exception.InvalidAccountStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountCreditTest {

    @Test
    void shouldIncreaseBalanceWhenCreditingActiveAccount() {

        // Given
        Account account = new Account(
                UUID.randomUUID(),
                Currency.COP
        );

        BigDecimal amount = new BigDecimal("150000.00");

        // When
        account.credit(amount);

        // Then
        assertEquals(
                0,
                new BigDecimal("150000.00")
                        .compareTo(account.getBalance())
        );
    }

    @Test
    void shouldRejectZeroCredit() {
        Account account = new Account(
                UUID.randomUUID(),
                Currency.COP
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> account.credit(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldRejectNegativeCredit() {
        Account account = new Account(
                UUID.randomUUID(),
                Currency.COP
        );

        BigDecimal amount = new BigDecimal("-50000.00");

        assertThrows(
                IllegalArgumentException.class,
                () -> account.credit(amount)
        );
    }

    @Test
    void shouldRejectNullCredit() {
        Account account = new Account(
                UUID.randomUUID(),
                Currency.COP
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> account.credit(null)
        );

    }

    @Test
    void shouldRejectCreditWhenAccountIsSuspended() {

        // Given
        Account account = new Account(
                UUID.randomUUID(),
                Currency.COP
        );

        account.suspend();

        BigDecimal amount = new BigDecimal("100000.00");

        // When / Then
        assertThrows(
                InvalidAccountStateException.class,
                () -> account.credit(amount)
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(account.getBalance())
        );
    }

    @Test
    void shouldAccumulateMultipleCredits() {

        Account account = new Account(
                UUID.randomUUID(),
                Currency.COP
        );

        account.credit(new BigDecimal("100000.00"));
        account.credit(new BigDecimal("50000.00"));

        assertEquals(
                0,
                new BigDecimal("150000.00")
                        .compareTo(account.getBalance())
        );
    }

    @Test
    void shouldRejectUnsupportedAmountScale() {
        Account account = new Account(
                UUID.randomUUID(),
                Currency.COP
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> account.credit(new BigDecimal("100.123"))
        );
    }
}
