package com.dfp.accounts.api;

import com.dfp.accounts.domain.exception.UnsupportedCurrencyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ProblemDetail handleUnsupportedCurrency(
            UnsupportedCurrencyException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.UNPROCESSABLE_CONTENT
        );

        problem.setTitle("Unprocessable Content");
        problem.setDetail(exception.getMessage());

        return problem;
    }
}
