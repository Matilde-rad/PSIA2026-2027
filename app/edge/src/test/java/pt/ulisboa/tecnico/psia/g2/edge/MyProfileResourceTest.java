package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MyProfileResourceTest {

    private static final String BODY = "{\"displayName\":\"Alice\",\"favoriteGenres\":[\"Horror\"]}";

    @InjectMock
    @RestClient
    ProfilesClient profiles;

    @InjectMock
    @RestClient
    RecommendationsClient recommendations;

    @InjectMock
    @RestClient
    WatchlistClient watchlist;

    @BeforeEach
    void emptyWatchlist() {
        when(watchlist.list(any())).thenReturn(Uni.createFrom().item(List.of()));
    }

    private static Profile saved(String id) {
        return new Profile(id, "Alice", null, List.of("Horror"), List.of(), List.of());
    }

    private static Response put(String token) {
        return RestAssured.given().auth().oauth2(token).contentType(ContentType.JSON).body(BODY)
                .put("/me/profile");
    }

    @Test
    void savesTheCallersProfileThenRefreshesTheirRecommendations() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(profiles.save(eq(id), any(), eq(bearer))).thenReturn(Uni.createFrom().item(saved(id)));
        when(watchlist.list(bearer)).thenReturn(Uni.createFrom().item(List.of(new MovieEntry("5", "liked", null))));
        Taste taste = new Taste(id, null, List.of("Horror"), List.of(), List.of(), List.of("5"), List.of());
        when(recommendations.refresh(id, taste, bearer)).thenReturn(Uni.createFrom().item(List.of()));

        put(token).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("userId", equalTo(id))
                .body("favoriteGenres", contains("Horror"));

        verify(recommendations).refresh(id, taste, bearer);
    }

    @Test
    void aFailingRefreshStillSavesTheProfile() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        when(profiles.save(eq(id), any(), any())).thenReturn(Uni.createFrom().item(saved(id)));
        when(recommendations.refresh(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("ai is down")));

        put(token).then().statusCode(200).body("userId", equalTo(id));
    }

    @Test
    void aSlowRefreshStillSavesTheProfile() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        when(profiles.save(eq(id), any(), any())).thenReturn(Uni.createFrom().item(saved(id)));
        when(recommendations.refresh(any(), any(), any())).thenReturn(Uni.createFrom().nothing());

        put(token).then().statusCode(200).body("userId", equalTo(id));
    }

    @Test
    void aValidationErrorFromUsersComesBackUnchangedAndNothingIsRefreshed() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        String error = "{\"error\":\"validation\",\"message\":\"invalid request\","
                + "\"fields\":[{\"field\":\"bio\",\"message\":\"size must be between 0 and 500\"}]}";
        when(profiles.save(any(), any(), any())).thenReturn(Uni.createFrom().failure(
                new ClientWebApplicationException(jakarta.ws.rs.core.Response.status(400).entity(error).build())));

        put(token).then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("fields[0].field", equalTo("bio"));

        verifyNoInteractions(recommendations);
    }

    @Test
    void noTokenIs401() {
        RestAssured.given().contentType(ContentType.JSON).body(BODY)
                .put("/me/profile")
                .then()
                .statusCode(401)
                .body("error", equalTo("unauthorized"));

        verifyNoInteractions(profiles, recommendations);
    }
}
