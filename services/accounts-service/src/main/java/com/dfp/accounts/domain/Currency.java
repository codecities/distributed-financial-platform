package com.dfp.accounts.domain;

import com.dfp.accounts.domain.exception.UnsupportedCurrencyException;

public enum Currency {
    COP,
    USD;

    public static Currency fromCode(String code) {
        try {
            return Currency.valueOf(code);
        } catch (IllegalArgumentException exception) {
            throw new UnsupportedCurrencyException(code);
        }
    }
}
