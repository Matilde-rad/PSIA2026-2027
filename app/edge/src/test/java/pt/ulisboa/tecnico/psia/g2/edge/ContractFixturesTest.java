package pt.ulisboa.tecnico.psia.g2.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;


@QuarkusTest
class ContractFixturesTest {
    private static final Path CONTRACTS = Path.of("../contracts");
    private static final ObjectMapper STRICT =
            new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @InjectMock
    @RestClient
    UsersClient users;

    @InjectMock
    @RestClient
    ProfilesClient profiles;

    @InjectMock
    @RestClient
    RecommendationsClient recommendations;

    private static Set<String> names(JsonNode node) {
        Set<String> names = new TreeSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    void meOutputMatchesTheUserFixture() throws Exception {
        Path file = CONTRACTS.resolve("user.json");
        User user = STRICT.readValue(file.toFile(), User.class);
        String token = Tokens.valid(user.id());
        when(users.me("Bearer " + token)).thenReturn(Uni.createFrom().item(user));

        String body = RestAssured.given()
                .auth().oauth2(token)
                .get("/me")
                .then().statusCode(200).extract().asString();

        assertEquals(names(MAPPER.readTree(file.toFile())), names(MAPPER.readTree(body)));
    }

    @Test
    void loginOutputMatchesTheLoginFixture() throws Exception {
        Path file = CONTRACTS.resolve("login-response.json");
        LoginResponse response = STRICT.readValue(file.toFile(), LoginResponse.class);
        when(users.login(any())).thenReturn(Uni.createFrom().item(response));

        String body = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"alice\",\"password\":\"password1\"}")
                .post("/auth/login")
                .then().statusCode(200).extract().asString();

        JsonNode expected = MAPPER.readTree(file.toFile());
        JsonNode actual = MAPPER.readTree(body);
        assertEquals(names(expected), names(actual));
        assertEquals(names(expected.get("user")), names(actual.get("user")));
    }

    @Test
    void savedProfileMatchesTheProfileFixture() throws Exception {
        Path file = CONTRACTS.resolve("profile.json");
        Profile profile = STRICT.readValue(file.toFile(), Profile.class);
        String token = Tokens.valid(profile.userId());
        when(profiles.save(any(), any(), any())).thenReturn(Uni.createFrom().item(profile));
        when(recommendations.refresh(any(), any(), any())).thenReturn(Uni.createFrom().item(List.of()));

        String body = RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(file.toFile())
                .put("/me/profile")
                .then().statusCode(200).extract().asString();

        assertEquals(names(MAPPER.readTree(file.toFile())), names(MAPPER.readTree(body)));
    }
}
