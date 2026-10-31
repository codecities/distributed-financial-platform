package com.dfp.accounts;

import com.dfp.accounts.api.GlobalExceptionHandler;
import com.dfp.accounts.application.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({
        GlobalExceptionHandler.class,
        AccountConcurrencyExceptionHandlerTest.TestController.class
})
public class AccountConcurrencyExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void shouldReturn409WhenOptimisticLockingFails() throws Exception {
        mockMvc.perform(get("/test/concurrent-update"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value(
                        "Account was modified by another transaction"
                ));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/concurrent-update")
        void simulateConcurrentUpdate() {
            throw new OptimisticLockingFailureException(
                    "Simulated stale account version"
            );
        }
    }
}
