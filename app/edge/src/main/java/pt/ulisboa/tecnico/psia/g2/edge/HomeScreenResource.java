package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
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
    @RestClient
    ProfilesClient profiles;

    @Inject
    JsonWebToken jwt;

    @GET
    public Uni<HomeView> get(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return recommendations.forUser(jwt.getSubject(), authorization).map(HomeView::new);
    }

    // Always the token's subject's own profile, never one sent by the browser.
    @POST
    @Path("/more")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<HomeView> more(MoreRequest request, @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        String userId = jwt.getSubject();
        List<String> exclude = request == null ? List.of() : request.exclude();
        return profiles.get(userId, authorization).onFailure(Upstream::notFound).recoverWithNull()
                .flatMap(profile -> profile == null
                        ? Uni.createFrom().item(new HomeView(List.of()))
                        : recommendations.more(userId, new MoreRequest(profile, exclude), authorization)
                                .onFailure(SearchScreenResource::aiUnavailable).transform(AiUnavailableException::new)
                                .map(HomeView::new));
    }
}
