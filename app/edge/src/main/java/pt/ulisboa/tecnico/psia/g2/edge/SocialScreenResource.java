package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/social")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SocialScreenResource {

    @Inject
    @RestClient
    SocialClient social;

    @Inject
    @RestClient
    ProfilesClient profiles;

    @Inject
    People people;

    @Inject
    JsonWebToken jwt;

    @GET
    public Uni<SocialView> get(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        Uni<Boolean> hasProfile = profiles.get(jwt.getSubject(), authorization)
                .map(p -> true)
                .onFailure(Upstream::notFound).recoverWithItem(false);
        Uni<List<PersonView>> suggestions = social.suggestions(authorization).chain(people::views);
        return Uni.combine().all().unis(hasProfile, suggestions).with(SocialView::new);
    }

    @GET
    @Path("/search")
    public Uni<List<PersonView>> search(@QueryParam("q") String q,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return social.search(q, authorization).chain(people::views);
    }
}
