package org.taskservice;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationIT {
    @LocalServerPort
    int port;

    @Test
    void contextLoads() {
    }

    @Test
    void testHealthCheck() {
        RestAssured.given()
                .baseUri("http://localhost")
                .port(port)
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
