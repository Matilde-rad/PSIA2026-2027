package pt.ulisboa.tecnico.psia.g2.edge;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@Path("/me/profile")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class MyProfileResource {

    private static final Logger LOG = Logger.getLogger(MyProfileResource.class);

    @Inject
    @RestClient
    ProfilesClient profiles;

    @Inject
    @RestClient
    RecommendationsClient recommendations;

    @Inject
    Tastes tastes;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "edge.profile.refresh-timeout")
    Duration refreshTimeout;

    // Saves the caller's profile, then has their recommendations recomputed from it so Home is
    // ready when they get there. The save is what matters: if the recompute fails or is slow,
    // the profile is still returned and the background agent catches up on its next run.
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Profile> save(Profile profile, @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        String userId = jwt.getSubject();
        return profiles.save(userId, profile, authorization)
                .call(saved -> tastes.of(saved, authorization)
                        .flatMap(taste -> recommendations.refresh(userId, taste, authorization))
                        .ifNoItem().after(refreshTimeout).fail()
                        .onFailure().recoverWithItem(t -> {
                            LOG.warnf("recommendations not refreshed after profile save error=%s",
                                    t.getClass().getSimpleName());
                            return List.of();
                        }));
    }
}
