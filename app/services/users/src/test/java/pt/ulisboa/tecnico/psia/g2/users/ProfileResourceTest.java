package pt.ulisboa.tecnico.psia.g2.users;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
        body.put("favoriteMovies", List.of("Alien"));
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
                .body("favoriteMovies", contains("Alien"));
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

    // genres and directors are random per test, so other tests' profiles never match
    @Test
    void listFiltersByGenreAndDirectorIgnoringCase() {
        Account alice = register();
        Account bob = register();
        String genre = "Genre" + uniqueUsername();
        String director = "Director" + uniqueUsername();
        create(alice, profile(genre, director)).then().statusCode(201);
        create(bob, profile(genre, "Someone Else")).then().statusCode(201);

        RestAssured.given().auth().oauth2(bob.token())
                .queryParam("genre", genre.toUpperCase())
                .get("/profiles")
                .then()
                .statusCode(200)
                .body("userId", hasItem(alice.id()))
                .body("userId", hasItem(bob.id()));

        RestAssured.given().auth().oauth2(bob.token())
                .queryParam("genre", genre)
                .queryParam("director", director.toLowerCase())
                .get("/profiles")
                .then()
                .statusCode(200)
                .body("userId", hasItem(alice.id()))
                .body("userId", not(hasItem(bob.id())));
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/profiles")
                .then()
                .statusCode(401)
                .body("error", equalTo("unauthorized"));
    }
}
