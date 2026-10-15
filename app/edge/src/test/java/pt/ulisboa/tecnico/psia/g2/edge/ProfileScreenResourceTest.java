package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProfileScreenResourceTest {

    @InjectMock
    @RestClient
    UsersClient users;

    @InjectMock
    @RestClient
    ProfilesClient profiles;

    @InjectMock
    @RestClient
    SocialClient social;

    @InjectMock
    @RestClient
    MoviesClient movies;

    @InjectMock
    @RestClient
    RecommendationsClient recommendations;

    // nobody follows anybody and the catalog knows Alien, unless a test says otherwise
    @BeforeEach
    void defaults() {
        when(social.stats(any(), any())).thenReturn(Uni.createFrom().item(new SocialStats(0, 0, false)));
        when(movies.lookup(any())).thenReturn(Uni.createFrom().item(List.of(ALIEN)));
    }

    private static final MoviesClient.CatalogMovie ALIEN =
            new MoviesClient.CatalogMovie("1214", "Alien (1979)", 1979, "http://img/poster", "http://img/banner");

    private static final Profile NO_PROFILE = null;

    private static Profile profileOf(String id) {
        return new Profile(id, "Alice", "likes films", List.of("Horror"), List.of("Ridley Scott"), List.of("1214", "404"));
    }

    private static <T> Uni<T> upstream404() {
        return Uni.createFrom().failure(new ClientWebApplicationException(
                Response.status(404).entity("{\"error\":\"not_found\"}").build()));
    }

    @Test
    void ownScreenIncludesRecommendations() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(users.get(id, bearer))
                .thenReturn(Uni.createFrom().item(new User(id, "alice", "2026-10-08T12:00:00Z")));
        when(profiles.get(id, bearer)).thenReturn(Uni.createFrom().item(profileOf(id)));
        when(social.stats(id, bearer)).thenReturn(Uni.createFrom().item(new SocialStats(3, 2, false)));
        when(recommendations.forUser(id, bearer))
                .thenReturn(Uni.createFrom().item(List.of(new Recommendation("m1", "Arrival", null, null))));

        Map<String, Object> body = RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/profile/" + id.toUpperCase(Locale.ROOT))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(id))
                .body("username", equalTo("alice"))
                .body("createdAt", equalTo("2026-10-08T12:00:00Z"))
                .body("profile.displayName", equalTo("Alice"))
                .body("profile.favoriteGenres", contains("Horror"))
                .body("profile.favoriteMovies", contains("1214", "404"))
                // ids become movies with posters; 404 is not in the catalog and is left out
                .body("favoriteMovies.movieId", contains("1214"))
                .body("favoriteMovies[0].title", equalTo("Alien (1979)"))
                .body("favoriteMovies[0].posterUrl", equalTo("http://img/poster"))
                .body("followers", equalTo(3))
                .body("following", equalTo(2))
                .body("recommendations.size()", equalTo(1))
                .body("recommendations[0].movieId", equalTo("m1"))
                .extract().as(new io.restassured.common.mapper.TypeRef<Map<String, Object>>() { });

        assertEquals(Set.of("id", "username", "createdAt", "profile", "favoriteMovies", "followers", "following",
                "followedByMe", "recommendations"), body.keySet());
        verify(users).get(id, bearer);
        verify(recommendations).forUser(id, bearer);
    }

    @Test
    void otherUsersScreenHasNoRecommendations() {
        String caller = UUID.randomUUID().toString();
        String other = UUID.randomUUID().toString();
        String token = Tokens.valid(caller);
        when(users.get(other, "Bearer " + token))
                .thenReturn(Uni.createFrom().item(new User(other, "bob", "2026-10-08T12:00:00Z")));
        when(profiles.get(other, "Bearer " + token)).thenReturn(Uni.createFrom().item(profileOf(other)));
        when(social.stats(other, "Bearer " + token)).thenReturn(Uni.createFrom().item(new SocialStats(1, 0, true)));

        RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/profile/" + other)
                .then()
                .statusCode(200)
                .body("username", equalTo("bob"))
                .body("profile.userId", equalTo(other))
                .body("followedByMe", equalTo(true))
                .body("recommendations", empty());

        verify(users).get(other, "Bearer " + token);
        verifyNoInteractions(recommendations);
    }

    @Test
    void anAccountWithoutAProfileHasANullProfile() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(users.get(id, bearer))
                .thenReturn(Uni.createFrom().item(new User(id, "alice", "2026-10-08T12:00:00Z")));
        when(profiles.get(id, bearer)).thenReturn(upstream404());
        when(recommendations.forUser(id, bearer)).thenReturn(Uni.createFrom().item(List.of()));

        RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/profile/" + id)
                .then()
                .statusCode(200)
                .body("username", equalTo("alice"))
                .body("profile", nullValue())
                .body("favoriteMovies", empty());

        verifyNoInteractions(movies);   // nothing to look up
    }

    @Test
    void followersAndFollowingAreListsOfPeopleWithSharedMovieTitles() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        Person bob = new Person(UUID.randomUUID().toString(), "bob", "Bob", true, List.of("Horror"), List.of(),
                List.of("1214"));
        when(social.followers(id, bearer)).thenReturn(Uni.createFrom().item(List.of(bob)));
        when(social.following(id, bearer)).thenReturn(Uni.createFrom().item(List.of()));

        RestAssured.given().auth().oauth2(token)
                .get("/screens/profile/" + id + "/followers")
                .then()
                .statusCode(200)
                .body("username", contains("bob"))
                .body("[0].followedByMe", equalTo(true))
                .body("[0].sharedGenres", contains("Horror"))
                .body("[0].sharedMovies", contains("Alien (1979)"));
        RestAssured.given().auth().oauth2(token)
                .get("/screens/profile/" + id + "/following")
                .then()
                .statusCode(200)
                .body("$", empty());
        RestAssured.given().auth().oauth2(token)
                .get("/screens/profile/nope/followers")
                .then()
                .statusCode(404);
    }

    @Test
    void anUnknownUserIs404() {
        String caller = UUID.randomUUID().toString();
        String other = UUID.randomUUID().toString();
        String token = Tokens.valid(caller);
        when(users.get(other, "Bearer " + token)).thenReturn(upstream404());
        when(profiles.get(other, "Bearer " + token)).thenReturn(upstream404());

        RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/profile/" + other)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void malformedIdIs404WithoutUpstreamCalls() {
        RestAssured.given()
                .auth().oauth2(Tokens.valid(UUID.randomUUID().toString()))
                .get("/screens/profile/not-a-uuid")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("not_found"));

        verifyNoInteractions(users, profiles, social, recommendations);
    }
}
