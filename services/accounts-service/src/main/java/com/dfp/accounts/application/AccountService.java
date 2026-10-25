package com.dfp.accounts.application;

import com.dfp.accounts.application.exception.AccountNotFoundException;
import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.Currency;
import com.dfp.accounts.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(
            AccountRepository accountRepository
    ) {
        this.accountRepository = accountRepository;
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
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        account.suspend();

        return account;
    }
}
