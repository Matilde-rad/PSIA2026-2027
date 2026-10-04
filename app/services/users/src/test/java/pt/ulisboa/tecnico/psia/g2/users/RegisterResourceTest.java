package pt.ulisboa.tecnico.psia.g2.users;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.post;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.uniqueUsername;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.valid;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class RegisterResourceTest {

    @Test
    void validRegistrationReturns201WithLocationAndNoHash() {
        String name = uniqueUsername();
        Map<String, Object> body = valid(name.toUpperCase());
        body.put("bio", "  hello  ");
        body.put("favoriteGenres", List.of("SCIENCE_FICTION", "ACTION", "DRAMA", "ACTION"));

        ExtractableResponse<Response> response = post(body)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("username", equalTo(name))
                .body("bio", equalTo("hello"))
                .body("favoriteGenres", contains("ACTION", "DRAMA", "SCIENCE_FICTION"))
                .body("createdAt", notNullValue())
                .body("id", notNullValue())
                .body("$", not(hasKey("passwordHash")))
                .body("$", not(hasKey("password")))
                .extract();

        String id = response.path("id");
        String location = response.header("Location");
        assertNotNull(location);
        assertTrue(location.startsWith("http"), "Location was " + location);
        assertTrue(location.endsWith("/users/" + id), "Location was " + location);
        assertFalse(response.asString().contains("$2"), "body must not contain a bcrypt hash");

        RestAssured.get("/users/" + id)
                .then()
                .statusCode(200)
                .body("createdAt", equalTo(response.<String>path("createdAt")))
                .body("favoriteGenres", contains("ACTION", "DRAMA", "SCIENCE_FICTION"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acceptedBoundaries")
    void boundariesAreAccepted(String label, Map<String, Object> body) {
        post(body).then().statusCode(201).contentType(ContentType.JSON);
    }

    static Stream<Arguments> acceptedBoundaries() {
        return Stream.of(
                Arguments.of("username 3 chars", valid(uniqueUsername(3))),
                Arguments.of("username 32 chars", valid(uniqueUsername(32))),
                Arguments.of("password 8 chars", with(valid(uniqueUsername()), "password", "12345678")),
                Arguments.of("password 72 ASCII chars", with(valid(uniqueUsername()), "password", "a".repeat(72))),
                Arguments.of("password exactly 72 UTF-8 bytes",
                        with(valid(uniqueUsername()), "password", "\u00e9".repeat(36))),
                Arguments.of("bio 500 chars", with(valid(uniqueUsername()), "bio", "x".repeat(500))));
    }

    @Test
    void blankBioIsStoredAsNull() {
        post(with(valid(uniqueUsername()), "bio", "   "))
                .then()
                .statusCode(201)
                .body("bio", nullValue());
    }

    @Test
    void absentOrNullGenresGiveEmptyList() {
        post(valid(uniqueUsername()))
                .then()
                .statusCode(201)
                .body("favoriteGenres", empty());

        post(with(valid(uniqueUsername()), "favoriteGenres", null))
                .then()
                .statusCode(201)
                .body("favoriteGenres", empty());
    }

    @Test
    void unknownPropertiesAreIgnored() {
        Map<String, Object> body = valid(uniqueUsername());
        body.put("id", "00000000-0000-0000-0000-000000000000");
        body.put("createdAt", "2000-01-01T00:00:00Z");
        body.put("passwordHash", "x");
        body.put("role", "admin");

        post(body)
                .then()
                .statusCode(201)
                .body("id", not(equalTo("00000000-0000-0000-0000-000000000000")))
                .body("createdAt", not(startsWith("2000-")))
                .body("$", not(hasKey("role")))
                .body("$", not(hasKey("passwordHash")));
    }

    private static Map<String, Object> with(Map<String, Object> body, String key, Object value) {
        body.put(key, value);
        return body;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void invalidInputReturns400NamingField(String label, Map<String, Object> body, String field) {
        String raw = post(body)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("validation"))
                .body("message", equalTo("invalid request"))
                .body("fields.field", hasItem(field))
                .body("fields.field", everyItem(not(containsString("."))))
                .body("fields.message", everyItem(not(emptyOrNullString())))
                .extract()
                .asString();
        // a rejected password must never be reflected back
        Object password = body.get("password");
        if (password instanceof String p) {
            assertFalse(raw.contains(p), "400 body must not echo the password: " + raw);
        }
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of("username too short", with(valid("ab"), "password", "password1"), "username"),
                Arguments.of("username too long", valid("a".repeat(33)), "username"),
                Arguments.of("username bad characters", valid("bad name!"), "username"),
                Arguments.of("username leading space", valid(" abc"), "username"),
                Arguments.of("username trailing space", valid("abc "), "username"),
                Arguments.of("password 7 chars", with(valid("abc"), "password", "1234567"), "password"),
                Arguments.of("bio 501 chars", with(valid("abc"), "bio", "x".repeat(501)), "bio"),
                Arguments.of("password 73 UTF-8 bytes",
                        with(valid("abc"), "password", "\u00e9".repeat(36) + "a"), "password"),
                Arguments.of("unknown genre", with(valid("abc"), "favoriteGenres", List.of("comedy")), "favoriteGenres"),
                Arguments.of("genre not exact enum name",
                        with(valid("abc"), "favoriteGenres", List.of("Sci-Fi")), "favoriteGenres"),
                Arguments.of("genre wrong case",
                        with(valid("abc"), "favoriteGenres", List.of("action")), "favoriteGenres"),
                Arguments.of("genres not an array", with(valid("abc"), "favoriteGenres", "ACTION"), "favoriteGenres"));
    }

    @Test
    void nullPasswordAndMissingUsernameBothReported() {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("password", null);
        post(body)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("fields.field", containsInAnyOrder("password", "username"));
    }

    @Test
    void byteLimitIsReportedWithOtherFieldErrors() {
        post(with(valid("ab"), "password", "\u00e9".repeat(36) + "a"))
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("fields.field", containsInAnyOrder("password", "username"));
    }

    @Test
    void malformedJsonReturns400() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"abc\", ")
                .post("/users")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("validation"))
                .body("message", equalTo("malformed JSON"))
                .body("fields", empty());
    }

    @Test
    void unknownGenreListsAllowedValues() {
        String raw = post(with(valid("abc"), "favoriteGenres", List.of("comedy")))
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("fields[0].field", equalTo("favoriteGenres"))
                .body("fields[0].message", startsWith("must be one of: ACTION"))
                .extract()
                .asString();
        assertFalse(raw.contains("comedy"), "400 body must not echo the rejected genre: " + raw);
    }

    @Test
    void missingBodyReturns400() {
        post(null)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("validation"))
                .body("fields", empty());
    }

    @Test
    void duplicateUsernameInAnyCaseReturns409() {
        String name = uniqueUsername();
        post(valid(name)).then().statusCode(201);

        for (String attempt : List.of(name, name.toUpperCase())) {
            String raw = post(valid(attempt))
                    .then()
                    .statusCode(409)
                    .contentType(ContentType.JSON)
                    .body("error", equalTo("conflict"))
                    .body("message", equalTo("username already taken"))
                    .body("$", not(hasKey("fields")))
                    .body("$", not(hasKey("id")))
                    .extract()
                    .asString();
            // a 409 must reveal nothing about the existing account
            assertFalse(raw.contains(name), "409 body must not echo the username: " + raw);
        }
    }

    @Test
    void unexpectedExceptionReturnsGeneric500() {
        String raw = RestAssured.get("/test/boom")
                .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("error", equalTo("internal"))
                .body("message", equalTo("unexpected error"))
                .extract()
                .asString();
        assertFalse(raw.contains("secret"), "body leaked exception message: " + raw);
        assertFalse(raw.contains("IllegalStateException"), "body leaked exception class: " + raw);
    }

    @Test
    void concurrentDuplicateGivesOne201AndOne409() throws Exception {
        String name = uniqueUsername();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return post(valid(name)).statusCode();
                }));
            }
            List<Integer> codes = new ArrayList<>();
            for (Future<Integer> f : futures) {
                codes.add(f.get(10, TimeUnit.SECONDS));
            }
            Collections.sort(codes);
            assertEquals(List.of(201, 409), codes);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void runsOnRandomPortNotDevPort() {
        assertNotEquals(8081, RestAssured.port);
    }
}
