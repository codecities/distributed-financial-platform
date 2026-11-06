package com.dfp.accounts.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AccountMovement {

    private UUID id;

    private UUID accountId;

    private UUID operationId;

    private MovementType type;

    private BigDecimal amount;

    private Currency currency;

    private Instant createdAt;

    public AccountMovement(
            UUID id,
            UUID accountId,
            UUID operationId,
            MovementType type,
            BigDecimal amount,
            Currency currency,
            Instant createdAt
    ) {
        validate(
                accountId,
                operationId,
                amount,
                currency
        );

        this.id = id;
        this.accountId = accountId;
        this.operationId = operationId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    private static void validate(
            UUID accountId,
            UUID operationId,
            BigDecimal amount,
            Currency currency
    ) {
        if (accountId == null) {
            throw new IllegalArgumentException(
                    "Account ID is required"
            );
        }

        if (operationId == null) {
            throw new IllegalArgumentException(
                    "Operation ID is required"
            );
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Movement amount must be greater than zero"
            );
        }

        BigDecimal normalized = amount.stripTrailingZeros();

        if (normalized.scale() > 2) {
            throw new IllegalArgumentException(
                    "Movement amount supports at most two decimal places"
            );
        }

        if (currency == null) {
            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }
    }

    public static AccountMovement credit(
            UUID accountId,
            UUID operationId,
            BigDecimal amount,
            Currency currency
    ) {
        return new AccountMovement(
                UUID.randomUUID(),
                accountId,
                operationId,
                MovementType.CREDIT,
                amount,
                currency,
                Instant.now()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public MovementType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
