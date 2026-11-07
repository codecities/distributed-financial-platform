package com.dfp.accounts.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_movements")
public class AccountMovement {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "operation_id", nullable = false, unique = true)
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",  nullable = false)
    private MovementType type;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AccountMovement() {
    }

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
