package pt.ulisboa.tecnico.psia.g2.edge;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/search")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SearchScreenResource {

    @Inject
    @RestClient
    RecommendationsClient recommendations;

    @Inject
    JsonWebToken jwt;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<SearchView> search(SearchRequest request,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return recommendations.search(jwt.getSubject(), request, authorization)
                .onFailure(SearchScreenResource::aiUnavailable).transform(AiUnavailableException::new);
    }

    // recommendations answers 503 when the model or the catalog could not answer.
    private static boolean aiUnavailable(Throwable t) {
        return t instanceof WebApplicationException e && e.getResponse().getStatus() == 503;
    }
}
