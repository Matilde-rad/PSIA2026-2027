package pt.ulisboa.tecnico.psia.g2.users;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// usernames are random per test, so tests need no cleanup and can run in any order
final class Registrations {

    private Registrations() {
    }

    static String uniqueUsername() {
        return "u" + hex().substring(0, 20);
    }

    static String uniqueUsername(int length) {
        return hex().substring(0, length);
    }

    static Map<String, Object> valid(String username) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", "password1");
        return body;
    }

    static Response post(Object body) {
        RequestSpecification request = RestAssured.given().contentType(ContentType.JSON);
        if (body != null) {
            request = request.body(body);
        }
        return request.post("/users");
    }

    private static String hex() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
