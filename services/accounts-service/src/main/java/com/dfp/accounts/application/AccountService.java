package com.dfp.accounts.application;

import com.dfp.accounts.domain.Account;
import com.dfp.accounts.domain.Currency;
import com.dfp.accounts.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
