package com.dfp.accounts.api;

import java.util.UUID;

public record CreateAccountRequest(
        UUID customerId,
        String currency
) {
}
