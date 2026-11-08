package com.dfp.accounts.application;

import com.dfp.accounts.application.exception.AccountNotFoundException;
import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.AccountMovement;
import com.dfp.accounts.domain.Currency;
import com.dfp.accounts.repository.AccountMovementRepository;
import com.dfp.accounts.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;

    public AccountService(
            AccountRepository accountRepository,
            AccountMovementRepository movementRepository
    ) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public Account createAccount(UUID customerId, Currency currency) {
        Account entity = new Account(customerId, currency);
        return accountRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<Account> findById(UUID id) {
        return accountRepository.findById(id);
    }

    @Transactional
    public Account suspendAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found" + accountId));

        account.suspend();

        return account;
    }

    @Transactional
    public void creditAccount(
            UUID accountId,
            UUID operationId,
            BigDecimal amount
    ) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found" + accountId));

        account.credit(amount);

        AccountMovement movement = AccountMovement.credit(
                account.getId(),
                operationId,
                amount,
                account.getCurrency()
        );

        movementRepository.save(movement);
    }
}
