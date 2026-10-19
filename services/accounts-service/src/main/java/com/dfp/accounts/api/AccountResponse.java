package com.dfp.accounts.api;

import com.dfp.accounts.domain.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String currency,
        AccountStatus status,
        Instant createdAt
) {
}
