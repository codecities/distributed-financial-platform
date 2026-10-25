package com.dfp.accounts.api;

import com.dfp.accounts.application.AccountService;
import com.dfp.accounts.application.exception.AccountNotFoundException;
import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.Currency;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(
            AccountService accountService
    ) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        Currency currency = Currency.fromCode(request.currency());

        Account account = accountService.createAccount(
                request.customerId(),
                currency
        );

        AccountResponse response = new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getCurrency().name(),
                account.getStatus(),
                account.getCreatedAt()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID id
    ) {
        AccountResponse accountResponse =
                accountService.findById(id).map(
                        acc -> new AccountResponse(
                                acc.getId(),
                                acc.getCustomerId(),
                                acc.getCurrency() != null ? acc.getCurrency().name() : null,
                                acc.getStatus(),
                                acc.getCreatedAt()
                        )
                ).orElseThrow(
                        () -> new AccountNotFoundException("Account not found: " + id)
                );

        return ResponseEntity.ok(accountResponse);
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<AccountResponse> suspendAccount(
            @PathVariable UUID id
    ) {
        Account account = accountService.suspendAccount(id);

        AccountResponse response = new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getCurrency().name(),
                account.getStatus(),
                account.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }
}
