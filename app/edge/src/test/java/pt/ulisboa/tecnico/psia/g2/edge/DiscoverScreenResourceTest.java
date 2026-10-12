package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DiscoverScreenResourceTest {

    @InjectMock
    @RestClient
    ProfilesClient profiles;

    @Test
    void discoverForwardsTheFiltersAndTheToken() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        String bearer = "Bearer " + token;
        Profile alice = new Profile(UUID.randomUUID().toString(), "Alice", "likes films", List.of("Horror"),
                List.of("Ridley Scott"), List.of("Alien"));
        when(profiles.list("horror", null, bearer)).thenReturn(Uni.createFrom().item(List.of(alice)));

        RestAssured.given()
                .auth().oauth2(token)
                .queryParam("genre", "horror")
                .get("/screens/discover")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].userId", equalTo(alice.userId()))
                .body("[0].favoriteGenres", contains("Horror"));

        verify(profiles).list("horror", null, bearer);
    }

    @Test
    void noTokenIs401() {
        RestAssured.given()
                .get("/screens/discover")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("error", equalTo("unauthorized"));

        verifyNoInteractions(profiles);
    }
}
