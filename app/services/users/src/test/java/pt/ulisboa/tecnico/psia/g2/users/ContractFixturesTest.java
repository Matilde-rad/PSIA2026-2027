package pt.ulisboa.tecnico.psia.g2.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.login;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.loginToken;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.post;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.uniqueUsername;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.valid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ContractFixturesTest {
    private static final Path CONTRACTS = Path.of("../../contracts");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode fixture(String file) throws Exception {
        return MAPPER.readTree(CONTRACTS.resolve(file).toFile());
    }

    private static Set<String> names(JsonNode node) {
        Set<String> names = new TreeSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    void registerAndMeMatchTheUserFixture() throws Exception {
        String name = uniqueUsername();
        String registered = post(valid(name)).then().statusCode(201).extract().asString();
        Set<String> expected = names(fixture("user.json"));
        assertEquals(expected, names(MAPPER.readTree(registered)));

        String me = RestAssured.given()
                .auth().oauth2(loginToken(name))
                .get("/users/me")
                .then().statusCode(200).extract().asString();
        assertEquals(expected, names(MAPPER.readTree(me)));
    }

    @Test
    void loginMatchesTheLoginFixture() throws Exception {
        String name = uniqueUsername();
        post(valid(name)).then().statusCode(201);

        JsonNode actual = MAPPER.readTree(
                login(name, "password1").then().statusCode(200).extract().asString());
        JsonNode expected = fixture("login-response.json");
        assertEquals(names(expected), names(actual));
        assertEquals(names(expected.get("user")), names(actual.get("user")));
    }

    @Test
    void savedProfileMatchesTheProfileFixture() throws Exception {
        String name = uniqueUsername();
        String id = post(valid(name)).then().statusCode(201).extract().path("id");

        String saved = RestAssured.given()
                .auth().oauth2(loginToken(name))
                .contentType("application/json")
                .body(CONTRACTS.resolve("profile.json").toFile())
                .put("/profiles/" + id)
                .then().statusCode(200).extract().asString();
        assertEquals(names(fixture("profile.json")), names(MAPPER.readTree(saved)));
    }
}
