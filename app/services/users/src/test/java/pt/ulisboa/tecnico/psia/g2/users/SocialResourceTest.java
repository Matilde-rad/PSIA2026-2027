package pt.ulisboa.tecnico.psia.g2.users;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
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
import io.restassured.specification.RequestSpecification;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// Genres, directors and movie ids are random per test, so other tests' profiles never overlap.
@QuarkusTest
class SocialResourceTest {

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

    private static String unique(String prefix) {
        return prefix + uniqueUsername();
    }

    private static String movieId() {
        return String.valueOf(100_000_000 + (int) (Math.random() * 800_000_000));
    }

    private static void saveProfile(Account account, String displayName, List<String> genres, List<String> directors,
            List<String> movies) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", displayName);
        body.put("favoriteGenres", genres);
        body.put("favoriteDirectors", directors);
        body.put("favoriteMovies", movies);
        as(account).contentType(ContentType.JSON).body(body)
                .put("/profiles/" + account.id())
                .then().statusCode(200);
    }

    private static void follow(Account follower, Account followee) {
        as(follower).put("/social/following/" + followee.id()).then().statusCode(204);
    }

    @Test
    void followShowsUpInStatsAndBothLists() {
        Account alice = register();
        Account bob = register();

        follow(alice, bob);
        follow(alice, bob);   // twice is fine and counts once

        as(alice).get("/social/" + bob.id() + "/stats").then()
                .statusCode(200)
                .body("followers", equalTo(1))
                .body("following", equalTo(0))
                .body("followedByMe", equalTo(true));
        as(bob).get("/social/" + alice.id() + "/stats").then()
                .body("followers", equalTo(0))
                .body("following", equalTo(1))
                .body("followedByMe", equalTo(false));

        as(bob).get("/social/" + bob.id() + "/followers").then()
                .statusCode(200)
                .body("id", contains(alice.id()))
                .body("username", contains(alice.name()))
                .body("followedByMe", contains(false));   // bob does not follow alice back
        as(alice).get("/social/" + alice.id() + "/following").then()
                .body("id", contains(bob.id()))
                .body("followedByMe", contains(true));
    }

    @Test
    void unfollowRemovesTheFollow() {
        Account alice = register();
        Account bob = register();
        follow(alice, bob);

        as(alice).delete("/social/following/" + bob.id()).then().statusCode(204);
        as(alice).delete("/social/following/" + bob.id()).then().statusCode(204);

        as(alice).get("/social/" + bob.id() + "/stats").then()
                .body("followers", equalTo(0))
                .body("followedByMe", equalTo(false));
    }

    @Test
    void followingYourselfIs400AndAnUnknownUserIs404() {
        Account alice = register();

        as(alice).put("/social/following/" + alice.id()).then()
                .statusCode(400)
                .body("error", equalTo("validation"));
        as(alice).put("/social/following/" + UUID.randomUUID()).then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
        as(alice).get("/social/" + UUID.randomUUID() + "/followers").then().statusCode(404);
    }

    @Test
    void suggestionsAreThePeopleWithTheMostInCommonFirst() {
        String genre = unique("Genre");
        String director = unique("Director");
        String movie = movieId();
        Account me = register();
        Account sameMovie = register();
        Account sameGenre = register();
        Account nothing = register();
        saveProfile(me, "Me", List.of(genre, "Other"), List.of(director), List.of(movie));
        saveProfile(sameMovie, "Movie", List.of(), List.of(), List.of(movie, movieId()));
        saveProfile(sameGenre, "Genre", List.of(genre.toUpperCase()), List.of(), List.of());
        saveProfile(nothing, "Nothing", List.of(unique("Genre")), List.of(), List.of());

        as(me).get("/social/suggestions").then()
                .statusCode(200)
                // a shared film outweighs a shared genre
                .body("id", contains(sameMovie.id(), sameGenre.id()))
                .body("[0].sharedMovies", contains(movie))
                .body("[0].sharedGenres", empty())
                .body("[1].sharedGenres", contains(genre.toUpperCase()))
                .body("[1].displayName", equalTo("Genre"))
                .body("[1].followedByMe", equalTo(false));
    }

    @Test
    void peopleIAlreadyFollowAreNotSuggested() {
        String genre = unique("Genre");
        Account me = register();
        Account other = register();
        saveProfile(me, "Me", List.of(genre), List.of(), List.of());
        saveProfile(other, "Other", List.of(genre), List.of(), List.of());
        follow(me, other);

        as(me).get("/social/suggestions").then()
                .statusCode(200)
                .body("id", not(hasItem(other.id())));
    }

    @Test
    void withoutAProfileThereAreNoSuggestions() {
        Account me = register();

        as(me).get("/social/suggestions").then().statusCode(200).body("$", empty());
    }

    @Test
    void searchFindsByUsernameOrDisplayNameAndNeverTheCaller() {
        Account me = register();
        Account byName = register();
        Account byDisplay = register();
        String display = unique("Zed");
        saveProfile(byDisplay, display, List.of(), List.of(), List.of());

        as(me).queryParam("q", byName.name().substring(0, 12).toUpperCase()).get("/social/search").then()
                .statusCode(200)
                .body("id", contains(byName.id()));
        as(me).queryParam("q", display.toLowerCase()).get("/social/search").then()
                .body("id", contains(byDisplay.id()))
                .body("displayName", contains(display));
        as(me).queryParam("q", me.name()).get("/social/search").then().body("$", empty());
        // one character is too short, and % is not a wildcard
        as(me).queryParam("q", "u").get("/social/search").then().body("$", empty());
        as(me).queryParam("q", "%%").get("/social/search").then().body("$", empty());
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/social/suggestions").then()
                .statusCode(401)
                .body("error", equalTo("unauthorized"));
        RestAssured.given().put("/social/following/" + UUID.randomUUID()).then().statusCode(401);
    }
}
