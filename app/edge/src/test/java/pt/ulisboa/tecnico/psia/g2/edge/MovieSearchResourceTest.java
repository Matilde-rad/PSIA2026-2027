package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MovieSearchResourceTest {

    @InjectMock
    @RestClient
    MoviesClient movies;

    @Test
    void returnsTheCatalogMatchesWithUiFieldNames() {
        when(movies.search("par", MovieSearchResource.SUGGESTIONS)).thenReturn(Uni.createFrom().item(List.of(
                new MoviesClient.CatalogMovie("202439", "Parasite (2019)", 2019, "http://img/p", "http://img/b"))));

        RestAssured.given().auth().oauth2(Tokens.valid(UUID.randomUUID().toString()))
                .queryParam("q", " par ")
                .get("/screens/movies/search")
                .then()
                .statusCode(200)
                .body("movieId", contains("202439"))
                .body("[0].title", equalTo("Parasite (2019)"))
                .body("[0].year", equalTo(2019))
                .body("[0].posterUrl", equalTo("http://img/p"));
    }

    @Test
    void oneCharacterIsNotSearched() {
        RestAssured.given().auth().oauth2(Tokens.valid(UUID.randomUUID().toString()))
                .queryParam("q", "p")
                .get("/screens/movies/search")
                .then()
                .statusCode(200)
                .body("$", empty());

        verifyNoInteractions(movies);
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/screens/movies/search?q=par").then().statusCode(401);

        verifyNoInteractions(movies);
    }
}
