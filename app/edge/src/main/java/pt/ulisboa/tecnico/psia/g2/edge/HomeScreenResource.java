package pt.ulisboa.tecnico.psia.g2.edge;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/home")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class HomeScreenResource {

    @Inject
    @RestClient
    RecommendationsClient recommendations;

    @Inject
    JsonWebToken jwt;

    @GET
    public Uni<HomeView> get(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return recommendations.forUser(jwt.getSubject(), authorization).map(HomeView::new);
    }
}
