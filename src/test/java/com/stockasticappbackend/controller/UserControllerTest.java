package com.stockasticappbackend.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTest {

    @LocalServerPort
    private int port;

    private String token;

    @BeforeEach
    void setup() {
        RestAssured.reset();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.useRelaxedHTTPSValidation();

        String body = """
                {
                    "email": "sriram@test.com",
                    "password": "Sriram@321"
                }
                """;

        this.token = given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/auth/login")
            .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    @Test
    void testGetProfile() {
        given()
                .port(port)
                .header("Authorization", "Bearer " + token)
            .when()
                .get("/users/me")
            .then()
                .statusCode(200)
                .body("email", notNullValue());
    }
}