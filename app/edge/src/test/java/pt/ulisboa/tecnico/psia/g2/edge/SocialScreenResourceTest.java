package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SocialScreenResourceTest {

    @InjectMock
    @RestClient
    SocialClient social;

    @InjectMock
    @RestClient
    ProfilesClient profiles;

    @InjectMock
    @RestClient
    MoviesClient movies;

    private static Person person(String name, List<String> sharedMovies) {
        return new Person(UUID.randomUUID().toString(), name, null, false, List.of("Horror"), List.of(),
                sharedMovies);
    }

    private static Profile profileOf(String id) {
        return new Profile(id, "Me", null, List.of("Horror"), List.of(), List.of("1214"));
    }

    @Test
    void suggestionsCarrySharedMovieTitlesFromOneLookup() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(profiles.get(id, bearer)).thenReturn(Uni.createFrom().item(profileOf(id)));
        when(social.suggestions(bearer)).thenReturn(Uni.createFrom().item(
                List.of(person("bob", List.of("1214", "2571")), person("carol", List.of("2571")))));
        when(movies.lookup(new MoviesClient.LookupRequest(List.of("1214", "2571"))))
                .thenReturn(Uni.createFrom().item(List.of(
                        new MoviesClient.CatalogMovie("1214", "Alien (1979)", 1979, null, null),
                        new MoviesClient.CatalogMovie("2571", "Matrix, The (1999)", 1999, null, null))));

        RestAssured.given().auth().oauth2(token)
                .get("/screens/social")
                .then()
                .statusCode(200)
                .body("hasProfile", equalTo(true))
                .body("suggestions.username", contains("bob", "carol"))
                .body("suggestions[0].sharedMovies", contains("Alien (1979)", "Matrix, The (1999)"))
                .body("suggestions[1].sharedMovies", contains("Matrix, The (1999)"))
                .body("suggestions[0].sharedGenres", contains("Horror"));
    }

    @Test
    void withoutAProfileTheScreenSaysSo() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(profiles.get(id, bearer)).thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(
                Response.status(404).entity("{\"error\":\"not_found\"}").build())));
        when(social.suggestions(bearer)).thenReturn(Uni.createFrom().item(List.of()));

        RestAssured.given().auth().oauth2(token)
                .get("/screens/social")
                .then()
                .statusCode(200)
                .body("hasProfile", equalTo(false))
                .body("suggestions", empty());

        verifyNoInteractions(movies);
    }

    @Test
    void searchForwardsTheTextAndTheToken() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(social.search("bo", "Bearer " + token))
                .thenReturn(Uni.createFrom().item(List.of(person("bob", List.of()))));

        RestAssured.given().auth().oauth2(token)
                .queryParam("q", "bo")
                .get("/screens/social/search")
                .then()
                .statusCode(200)
                .body("username", contains("bob"))
                .body("[0].sharedMovies", empty());
    }

    @Test
    void failingUsersIs502() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(profiles.get(any(), any())).thenReturn(Uni.createFrom().item(profileOf("x")));
        when(social.suggestions(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("users is down")));

        RestAssured.given().auth().oauth2(token)
                .get("/screens/social")
                .then()
                .statusCode(502)
                .body("error", equalTo("bad_gateway"));
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/screens/social").then().statusCode(401);
        RestAssured.get("/screens/social/search?q=bo").then().statusCode(401);

        verifyNoInteractions(social, profiles, movies);
    }
}
