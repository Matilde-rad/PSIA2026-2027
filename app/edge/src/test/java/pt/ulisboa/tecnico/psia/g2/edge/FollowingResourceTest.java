package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FollowingResourceTest {

    @InjectMock
    @RestClient
    SocialClient social;

    @Test
    void followAndUnfollowForwardTheTokenAndAnswer204() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        String bearer = "Bearer " + token;
        String other = UUID.randomUUID().toString();
        when(social.follow(other, bearer)).thenReturn(Uni.createFrom().item(Response.noContent().build()));
        when(social.unfollow(other, bearer)).thenReturn(Uni.createFrom().item(Response.noContent().build()));

        RestAssured.given().auth().oauth2(token).put("/me/following/" + other).then().statusCode(204);
        RestAssured.given().auth().oauth2(token).delete("/me/following/" + other).then().statusCode(204);

        verify(social).follow(other, bearer);
        verify(social).unfollow(other, bearer);
    }

    @Test
    void anErrorFromUsersComesBackUnchanged() {
        String me = UUID.randomUUID().toString();
        String token = Tokens.valid(me);
        when(social.follow(me, "Bearer " + token)).thenReturn(Uni.createFrom().failure(
                new ClientWebApplicationException(Response.status(400)
                        .entity("{\"error\":\"validation\",\"message\":\"you cannot follow yourself\"}").build())));

        RestAssured.given().auth().oauth2(token)
                .put("/me/following/" + me)
                .then()
                .statusCode(400)
                .body("message", equalTo("you cannot follow yourself"));
    }

    @Test
    void noTokenIs401() {
        RestAssured.given().put("/me/following/" + UUID.randomUUID()).then().statusCode(401);

        verifyNoInteractions(social);
    }
}
