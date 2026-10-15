package pt.ulisboa.tecnico.psia.g2.users;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.loginToken;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.post;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.uniqueUsername;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.valid;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// Movie ids are random per test, so the internal list can be checked without other tests' rows.
@QuarkusTest
class WatchlistResourceTest {

    private record Account(String id, String name, String token) {
    }

    private static Account register() {
        String name = uniqueUsername();
        String id = post(valid(name)).then().statusCode(201).extract().path("id");
        return new Account(id, name, loginToken(name));
    }

    private static RequestSpecification as(Account account) {
        return RestAssured.given().auth().oauth2(account.token());
    }

    private static String movieId() {
        return String.valueOf(100_000_000 + (int) (Math.random() * 800_000_000));
    }

    private static void mark(Account account, String movie, String status) {
        as(account).contentType(ContentType.JSON).body(Map.of("status", status))
                .put("/watchlist/" + movie)
                .then().statusCode(200);
    }

    @Test
    void aMovieGoesOnTheListAndThenGetsAVerdict() {
        Account alice = register();
        String first = movieId();
        String second = movieId();

        as(alice).contentType(ContentType.JSON).body(Map.of("status", "to_watch"))
                .put("/watchlist/" + first)
                .then()
                .statusCode(200)
                .body("movieId", equalTo(first))
                .body("status", equalTo("to_watch"))
                .body("updatedAt", notNullValue());
        mark(alice, second, "to_watch");
        mark(alice, first, "liked");

        as(alice).get("/watchlist").then()
                .statusCode(200)
                .body("movieId", contains(first, second))
                .body("status", contains("liked", "to_watch"));
        as(alice).get("/watchlist/" + first).then()
                .statusCode(200)
                .body("status", equalTo("liked"));
    }

    @Test
    void removingTakesTheMovieOffTheList() {
        Account alice = register();
        String movie = movieId();
        mark(alice, movie, "to_watch");

        as(alice).delete("/watchlist/" + movie).then().statusCode(204);
        as(alice).delete("/watchlist/" + movie).then().statusCode(204);

        as(alice).get("/watchlist/" + movie).then().statusCode(404).body("error", equalTo("not_found"));
        as(alice).get("/watchlist").then().body("$", empty());
    }

    @Test
    void listsArePerUser() {
        Account alice = register();
        Account bob = register();
        mark(alice, movieId(), "to_watch");

        as(bob).get("/watchlist").then().statusCode(200).body("$", empty());
    }

    @Test
    void anUnknownStatusIs400AndABadMovieIdIs404() {
        Account alice = register();

        as(alice).contentType(ContentType.JSON).body(Map.of("status", "loved"))
                .put("/watchlist/" + movieId())
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("fields.field", contains("status"));
        as(alice).contentType(ContentType.JSON).body(Map.of("status", "liked"))
                .put("/watchlist/alien")
                .then().statusCode(404);
    }

    @Test
    void theFeedShowsWhatThePeopleIFollowDid() {
        Account me = register();
        Account followed = register();
        Account stranger = register();
        String wanted = movieId();
        String disliked = movieId();
        as(me).put("/social/following/" + followed.id()).then().statusCode(204);
        as(followed).contentType(ContentType.JSON).body(Map.of("displayName", "Followed"))
                .put("/profiles/" + followed.id()).then().statusCode(200);
        mark(followed, wanted, "to_watch");
        mark(followed, disliked, "disliked");
        mark(stranger, movieId(), "liked");
        mark(me, movieId(), "liked");

        as(me).get("/activity/feed").then()
                .statusCode(200)
                .body("movieId", contains(disliked, wanted))
                .body("status", contains("disliked", "to_watch"))
                .body("userId", contains(followed.id(), followed.id()))
                .body("[0].username", equalTo(followed.name()))
                .body("[0].displayName", equalTo("Followed"))
                .body("[0].at", notNullValue());
        as(stranger).get("/activity/feed").then().statusCode(200).body("$", empty());
    }

    @Test
    void anyoneLoggedInCanReadAUsersActivity() {
        Account alice = register();
        Account bob = register();
        String movie = movieId();
        mark(alice, movie, "liked");

        as(bob).get("/activity/users/" + alice.id()).then()
                .statusCode(200)
                .body("movieId", contains(movie))
                .body("status", contains("liked"))
                .body("username", contains(alice.name()));
        as(bob).get("/activity/users/" + UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    void theInternalListHasWatchedMoviesOnlyAndNeedsTheSharedKey() {
        Account alice = register();
        String liked = movieId();
        String disliked = movieId();
        String wanted = movieId();
        mark(alice, liked, "liked");
        mark(alice, disliked, "disliked");
        mark(alice, wanted, "to_watch");

        RestAssured.given().header("X-Internal-Key", "test-internal-key")
                .get("/internal/watched")
                .then()
                .statusCode(200)
                .body("find { it.movieId == '" + liked + "' }.liked", equalTo(true))
                .body("find { it.movieId == '" + liked + "' }.userId", equalTo(alice.id()))
                .body("find { it.movieId == '" + liked + "' }.watchedAt", notNullValue())
                .body("find { it.movieId == '" + disliked + "' }.liked", equalTo(false))
                .body("movieId", not(hasItem(wanted)));

        RestAssured.get("/internal/watched").then().statusCode(401);
        RestAssured.given().header("X-Internal-Key", "wrong").get("/internal/watched").then().statusCode(401);
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/watchlist").then().statusCode(401).body("error", equalTo("unauthorized"));
        RestAssured.given().delete("/watchlist/1214").then().statusCode(401);
        RestAssured.get("/activity/feed").then().statusCode(401);
    }
}
