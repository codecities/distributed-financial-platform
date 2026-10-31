package com.dfp.accounts.domain;

import com.dfp.accounts.domain.exception.InvalidAccountStateException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Account() {
    }

    public Account(
            UUID customerId,
            Currency currency
    ) {
        this.customerId = Objects.requireNonNull(
                customerId,
                "customerId is required"
        );

        this.currency = Objects.requireNonNull(
                currency,
                "currency is required"
        );

        this.id = UUID.randomUUID();
        this.status = AccountStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void suspend() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException(
                    "Only active accounts can be suspended"
            );
        }

        this.status = AccountStatus.SUSPENDED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
}
