package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/feed")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class FeedScreenResource {

    @Inject
    @RestClient
    ActivityClient activity;

    @Inject
    Activities activities;

    @GET
    public Uni<List<ActivityView>> get(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return activity.feed(authorization).chain(activities::views);
    }
}
