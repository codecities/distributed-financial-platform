package com.dfp.accounts.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AccountMovementTest {

    @Test
    void shouldCreateCreditMovement() {

        UUID accountId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        AccountMovement movement = AccountMovement.credit(
                accountId,
                operationId,
                new BigDecimal("1500000.00"),
                Currency.COP
        );

        assertNotNull(movement.getId());
        assertEquals(accountId, movement.getAccountId());
        assertEquals(operationId, movement.getOperationId());
        assertEquals(MovementType.CREDIT, movement.getType());

        assertEquals(
                0,
                new BigDecimal("1500000.00")
                        .compareTo(movement.getAmount())
        );

        assertEquals(Currency.COP, movement.getCurrency());
        assertNotNull(movement.getCreatedAt());
    }

    @Test
    void shouldRejectZeroOrNegativeMovementAmount() {

        UUID accountId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> AccountMovement.credit(
                        accountId,
                        operationId,
                        BigDecimal.ZERO,
                        Currency.COP
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AccountMovement.credit(
                        accountId,
                        operationId,
                        new BigDecimal("-100.00"),
                        Currency.COP
                )
        );
    }

    @Test
    void shouldRejectNullMovementAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AccountMovement.credit(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        Currency.COP
                )
        );
    }

    @Test
    void shouldRejectNullOperationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AccountMovement.credit(
                        UUID.randomUUID(),
                        null,
                        new BigDecimal("100000.00"),
                        Currency.COP
                )
        );
    }

    @Test
    void shouldRejectNullAccountId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AccountMovement.credit(
                        null,
                        UUID.randomUUID(),
                        new BigDecimal("100000.00"),
                        Currency.COP
                )
        );
    }

    @Test
    void shouldRejectNullCurrency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AccountMovement.credit(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100000.00"),
                        null
                )
        );
    }

    @Test
    void shouldRejectUnsupportedAmountScale() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AccountMovement.credit(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100.123"),
                        Currency.COP
                )
        );
    }
}
