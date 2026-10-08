package pt.ulisboa.tecnico.psia.g2.users;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.loginToken;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.post;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.uniqueUsername;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.valid;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class RegisterResourceTest {

    @Test
    void validRegistrationReturns201WithoutPasswordHash() {
        String name = uniqueUsername();
        String id = post(valid(name.toUpperCase(Locale.ROOT)))
                .then()
                .statusCode(201)
                .header("Location", notNullValue())     // URL of the new user
                .body("id", notNullValue())
                .body("username", equalTo(name))        // stored lowercase
                .body("createdAt", notNullValue())
                .body("$", not(hasKey("passwordHash")))
                .extract()
                .path("id");

        // the new user can be fetched (with a token) and looks the same
        RestAssured.given()
                .auth().oauth2(loginToken(name))
                .get("/users/" + id)
                .then()
                .statusCode(200)
                .body("username", equalTo(name));
    }

    // each invalid field is named in the 400 response
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void invalidInputReturns400NamingField(String label, Map<String, Object> body, String field) {
        post(body)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("validation"))
                .body("fields.field", hasItem(field));
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of("username too short", valid("ab"), "username"),
                Arguments.of("username bad characters", valid("bad name!"), "username"),
                Arguments.of("password too short", with(valid("abc"), "password", "1234567"), "password"));
    }

    @Test
    void malformedJsonReturns400() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"abc\", ")
                .post("/users")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("fields", empty());
    }

    // usernames are case-insensitive, so "ALICE" clashes with "alice"
    @Test
    void duplicateUsernameInAnyCaseReturns409() {
        String name = uniqueUsername();
        post(valid(name)).then().statusCode(201);

        post(valid(name.toUpperCase(Locale.ROOT)))
                .then()
                .statusCode(409)
                .body("error", equalTo("conflict"))
                .body("message", equalTo("username already taken"));
    }

    private static Map<String, Object> with(Map<String, Object> body, String key, Object value) {
        body.put(key, value);
        return body;
    }
}
