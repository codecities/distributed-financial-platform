package com.dfp.accounts;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
public class AccountIntegrationTest {

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

    @Test
    void shouldRejectUnsupportedCurrency() {

        // Given
        UUID customerId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "customerId", customerId.toString(),
                "currency", "EUR"
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
                .statusCode(422)
                .contentType("application/problem+json")
                .body("status", equalTo(422))
                .body("title", equalTo("Unprocessable Content"))
                .body("detail", equalTo("Unsupported currency: EUR"));
    }


    @Test
    void shouldRejectMissingCustomerId() {

        // Given
        Map<String, Object> request = Map.of(
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
                .statusCode(400);
    }

    @Test
    void shouldRejectMissingCurrency() {

        UUID customerId = UUID.randomUUID();

        // Given
        Map<String, Object> request = Map.of(
                "customerId", customerId
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
                .statusCode(400);
    }

    @Test
    void shouldRetrieveExistingAccount() {

        String customerId = UUID.randomUUID().toString();
        var response = createAccount(customerId, "COP");
        response.then().statusCode(201);
        String accountId = response.jsonPath().getString("id");

        given()
                .port(port)
                .accept(ContentType.JSON)
                .when()
                .get("/api/v1/accounts/" + accountId)
                .then()
                .statusCode(200)
                .body("id", equalTo(accountId))
                .body("customerId", equalTo(customerId))
                .body("currency", equalTo("COP"))
                .body("status", equalTo("ACTIVE"))
                .body("createdAt", notNullValue());
    }

    @Test
    void shouldReturn404WhenAccountDoesNotExist() {

        // Given
        UUID accountId = UUID.randomUUID();

        // When
        var response = given()
                .port(port)
                .accept(ContentType.JSON)
                .when()
                .get("/api/v1/accounts/" + accountId);

        // Then
        response.then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("status", equalTo(404))
                .body("title", equalTo("Not Found"))
                .body(
                        "detail",
                        equalTo("Account not found: " + accountId)
                );

    }

    @Test
    void shouldSuspendActiveAccount() {
        // Given
        String customerId = UUID.randomUUID().toString();
        var result = createAccount(customerId, "COP");
        result.then().statusCode(201);
        String accountId = result.jsonPath().getString("id");

        // When
        var response = given()
                .port(port)
                .when()
                .patch("/api/v1/accounts/{id}/suspend", accountId);

        // Then
        response.then()
                .statusCode(200)
                .body("id", equalTo(accountId))
                .body("status", equalTo("SUSPENDED"));

    }

    @Test
    void shouldRejectSuspendingAnAlreadySuspendedAccount() {
        // Given
        String customerId = UUID.randomUUID().toString();
        var result = createAccount(customerId, "COP");
        result.then().statusCode(201);
        String accountId = result.jsonPath().getString("id");

        given()
                .port(port)
                .when()
                .patch("/api/v1/accounts/{id}/suspend", accountId)
                .then()
                .statusCode(200);

        var response = given()
                .port(port)
                .when()
                .patch("/api/v1/accounts/{id}/suspend", accountId);

        response.then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("status", equalTo(409))
                .body(
                        "detail",
                        equalTo("Only active accounts can be suspended")
                );

        // Verify persisted state
        given()
                .port(port)
                .when()
                .get("/api/v1/accounts/{id}", accountId)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"));

    }

    private Response createAccount(String accountId, String currency) {
        Map<String, Object> request = Map.of(
                "customerId", accountId,
                "currency", currency
        );

        return given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/accounts");
    }

}
