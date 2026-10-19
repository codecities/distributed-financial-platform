package com.dfp.accounts;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
public class AccountCreationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");


    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateAccountSuccessfully() {

        // Given
        UUID customerId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "customerId", customerId,
                "currency", "COP"
        );

        // When
        var response = given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/accounts");

        // Then
        response.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("customerId", equalTo(customerId.toString()))
                .body("currency", equalTo("COP"))
                .body("status", equalTo("ACTIVE"))
                .body("createdAt", notNullValue());

        String accountId = response.jsonPath().getString("id");
        String createdAt = response.jsonPath().getString("createdAt");

        UUID.fromString(accountId);
        Instant.parse(createdAt);

        response.then()
                .header(
                        "Location",
                        endsWith("/api/v1/accounts/" + accountId)
                );

        // verify persistence
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM accounts
                        WHERE id = ?
                        AND customer_id = ?
                        AND currency = ?
                        AND status = ?
                        """,
                Integer.class,
                UUID.fromString(accountId),
                customerId,
                "COP",
                "ACTIVE"
        );

        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }
}
