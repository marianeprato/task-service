package org.taskservice.exception;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the database-outage handling end to end: the actual Postgres
 * container is stopped mid-test (not mocked), and the request must fail
 * fast with a clean 503 rather than hanging for HikariCP's old 30s default
 * or leaking a bare 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DatabaseOutageHandlingIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int port;

    @Test
    void returnsServiceUnavailableQuicklyWhenDatabaseIsDown() {
        postgres.stop();

        long start = System.currentTimeMillis();
        Response response = RestAssured.given()
                .baseUri("http://localhost")
                .port(port)
                .when()
                .get("/tasks");
        long elapsedMs = System.currentTimeMillis() - start;

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.jsonPath().getString("error"))
                .isEqualTo("Service temporarily unavailable, please try again shortly");
        // HikariCP's connection-timeout is 5s; this stays well clear of the
        // old 30s default while giving the assertion some margin.
        assertThat(elapsedMs).isLessThan(8000);
    }
}
