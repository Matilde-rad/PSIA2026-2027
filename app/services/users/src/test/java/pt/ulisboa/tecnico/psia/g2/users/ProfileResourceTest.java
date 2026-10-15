package pt.ulisboa.tecnico.psia.g2.users;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.loginToken;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.post;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.uniqueUsername;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.valid;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProfileResourceTest {

    private record Account(String id, String token) {
    }

    private static Account register() {
        String name = uniqueUsername();
        String id = post(valid(name)).then().statusCode(201).extract().path("id");
        return new Account(id, loginToken(name));
    }

    private static Map<String, Object> profile(String genre, String director) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Alice");
        body.put("bio", "likes films");
        body.put("favoriteGenres", List.of(genre));
        body.put("favoriteDirectors", List.of(director));
        body.put("favoriteMovies", List.of("1214"));
        return body;
    }

    private static Response create(Account account, Object body) {
        return RestAssured.given().auth().oauth2(account.token()).contentType(ContentType.JSON).body(body)
                .post("/profiles");
    }

    @Test
    void createThenGet() {
        Account alice = register();

        create(alice, profile("Horror", "Ridley Scott")).then()
                .statusCode(201)
                .body("userId", equalTo(alice.id()));

        RestAssured.given().auth().oauth2(alice.token())
                .get("/profiles/" + alice.id())
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Alice"))
                .body("favoriteGenres", contains("Horror"))
                .body("favoriteMovies", contains("1214"));
    }

    // the id in the body is ignored: a profile always belongs to the caller
    @Test
    void createIgnoresTheBodyUserId() {
        Account alice = register();
        Account bob = register();
        Map<String, Object> body = profile("Drama", "Agnes Varda");
        body.put("userId", bob.id());

        create(alice, body).then().statusCode(201).body("userId", equalTo(alice.id()));
    }

    @Test
    void secondCreateIs409() {
        Account alice = register();
        create(alice, profile("Horror", "Ridley Scott")).then().statusCode(201);

        create(alice, profile("Horror", "Ridley Scott")).then()
                .statusCode(409)
                .body("error", equalTo("conflict"));
    }

    @Test
    void missingListsBecomeEmpty() {
        Account alice = register();

        create(alice, Map.of("displayName", "Alice")).then()
                .statusCode(201)
                .body("favoriteGenres.size()", equalTo(0));
    }

    @Test
    void tooLongBioIs400() {
        Account alice = register();
        Map<String, Object> body = profile("Horror", "Ridley Scott");
        body.put("bio", "x".repeat(501));

        create(alice, body).then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("fields.field", hasItem("bio"));
    }

    // favorite movies are catalog ids; a title is rejected
    @Test
    void aFavoriteMovieThatIsNotAnIdIs400() {
        Account alice = register();
        Map<String, Object> body = profile("Horror", "Ridley Scott");
        body.put("favoriteMovies", List.of("Alien"));

        create(alice, body).then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("fields.field", hasItem("favoriteMovies"));
    }

    @Test
    void moreThanTenFavoriteMoviesIs400() {
        Account alice = register();
        Map<String, Object> body = profile("Horror", "Ridley Scott");
        body.put("favoriteMovies", List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"));

        create(alice, body).then()
                .statusCode(400)
                .body("fields.field", hasItem("favoriteMovies"));
    }

    @Test
    void ownerUpdatesAndDeletes() {
        Account alice = register();
        create(alice, profile("Horror", "Ridley Scott")).then().statusCode(201);

        RestAssured.given().auth().oauth2(alice.token()).contentType(ContentType.JSON)
                .body(profile("Comedy", "Elaine May"))
                .put("/profiles/" + alice.id())
                .then()
                .statusCode(200)
                .body("favoriteGenres", contains("Comedy"));

        RestAssured.given().auth().oauth2(alice.token())
                .delete("/profiles/" + alice.id())
                .then()
                .statusCode(204);

        RestAssured.given().auth().oauth2(alice.token())
                .get("/profiles/" + alice.id())
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void putCreatesTheProfileWhenThereIsNone() {
        Account alice = register();

        RestAssured.given().auth().oauth2(alice.token()).contentType(ContentType.JSON)
                .body(profile("Comedy", "Elaine May"))
                .put("/profiles/" + alice.id())
                .then()
                .statusCode(200)
                .body("userId", equalTo(alice.id()))
                .body("favoriteGenres", contains("Comedy"));

        RestAssured.given().auth().oauth2(alice.token())
                .get("/profiles/" + alice.id())
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Alice"));
    }

    @Test
    void internalProfilesNeedTheSharedKey() {
        Account alice = register();
        create(alice, profile("Horror", "Ridley Scott")).then().statusCode(201);

        RestAssured.given().header("X-Internal-Key", "test-internal-key")
                .get("/internal/profiles")
                .then()
                .statusCode(200)
                .body("userId", hasItem(alice.id()));

        RestAssured.get("/internal/profiles").then().statusCode(401);
        RestAssured.given().header("X-Internal-Key", "wrong")
                .get("/internal/profiles")
                .then()
                .statusCode(401);
        // a user token is not the key
        RestAssured.given().auth().oauth2(alice.token())
                .get("/internal/profiles")
                .then()
                .statusCode(401);
    }

    @Test
    void anotherUserCannotUpdateOrDelete() {
        Account alice = register();
        Account bob = register();
        create(alice, profile("Horror", "Ridley Scott")).then().statusCode(201);

        RestAssured.given().auth().oauth2(bob.token()).contentType(ContentType.JSON)
                .body(profile("Comedy", "Elaine May"))
                .put("/profiles/" + alice.id())
                .then()
                .statusCode(403)
                .body("error", equalTo("forbidden"));

        RestAssured.given().auth().oauth2(bob.token())
                .delete("/profiles/" + alice.id())
                .then()
                .statusCode(403);
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/profiles/" + UUID.randomUUID())
                .then()
                .statusCode(401)
                .body("error", equalTo("unauthorized"));
    }
}
