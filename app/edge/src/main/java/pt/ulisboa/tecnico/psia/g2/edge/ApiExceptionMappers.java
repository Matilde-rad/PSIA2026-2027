package pt.ulisboa.tecnico.psia.g2.edge;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ApiExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMappers.class);

    @ServerExceptionMapper
    public Response upstream(ClientWebApplicationException e) {
        int status = e.getResponse().getStatus();
        if (status >= 500) {
            return badGateway(e);
        }
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(e.getResponse().readEntity(String.class))
                .build();
    }

    @ServerExceptionMapper
    public Response web(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        if (status >= 500) {
            return badGateway(e);
        }
        if (status == 400) {
            return json(400, new ApiError("validation", "malformed JSON"));
        }
        return json(status, new ApiError(codeFor(status), e.getMessage()));
    }

    @ServerExceptionMapper
    public Response malformedJson(MismatchedInputException e) {
        return json(400, new ApiError("validation", "malformed JSON"));
    }

    @ServerExceptionMapper
    public Response authenticationFailed(AuthenticationFailedException e) {
        return json(401, new ApiError("unauthorized", "authentication required"));
    }

    @ServerExceptionMapper
    public Response unauthorized(UnauthorizedException e) {
        return Response.status(401)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError("unauthorized", "authentication required"))
                .build();
    }

    @ServerExceptionMapper
    public Response aiUnavailable(AiUnavailableException e) {
        return json(503, new ApiError("ai_unavailable", "the AI is unavailable, try again"));
    }

    @ServerExceptionMapper
    public Response unexpected(Throwable t) {
        return badGateway(t);
    }

    private static Response badGateway(Throwable t) {
        LOG.error("upstream call failed", t);
        return json(502, new ApiError("bad_gateway", "upstream service unavailable"));
    }

    static String codeFor(int status) {
        return switch (status) {
            case 401 -> "unauthorized";
            case 404 -> "not_found";
            case 409 -> "conflict";
            default -> "validation";
        };
    }

    private static Response json(int status, ApiError body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}
