package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
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
class GenresScreenResourceTest {

    @InjectMock
    @RestClient
    MoviesClient movies;

    @Test
    void returnsTheCatalogGenres() {
        when(movies.genres()).thenReturn(Uni.createFrom().item(List.of("Action", "Drama")));

        RestAssured.given()
                .auth().oauth2(Tokens.valid(UUID.randomUUID().toString()))
                .get("/screens/genres")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", contains("Action", "Drama"));
    }

    @Test
    void noTokenIs401() {
        RestAssured.given()
                .get("/screens/genres")
                .then()
                .statusCode(401)
                .body("error", equalTo("unauthorized"));

        verifyNoInteractions(movies);
    }
}
