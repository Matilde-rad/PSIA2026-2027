package pt.ulisboa.tecnico.psia.g2.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ApiExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMappers.class);

    @ServerExceptionMapper
    public Response web(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        if (status >= 500) {
            LOG.error("server error", e);
        }
        if (status == 400 && e.getCause() instanceof JsonProcessingException) {
            return json(400, new ApiError("validation", "malformed JSON", List.of()));
        }
        return json(status, ApiError.of(codeFor(status), status >= 500 ? "unexpected error" : e.getMessage()));
    }

    @ServerExceptionMapper
    public Response mismatched(MismatchedInputException e) {
        String field = e.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(n -> n != null)
                .findFirst()
                .orElse(null);
        if (field == null) {
            return json(400, new ApiError("validation", "invalid request", List.of()));
        }
        String message = "invalid value";
        if (e instanceof InvalidFormatException ife && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            message = "must be one of: " + Arrays.stream(ife.getTargetType().getEnumConstants())
                    .map(c -> ((Enum<?>) c).name())
                    .sorted()
                    .collect(Collectors.joining(", "));
        }
        return json(400, new ApiError("validation", "invalid request",
                List.of(new ApiError.FieldError(field, message))));
    }

    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e) {
        List<ApiError.FieldError> fields = e.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldError(jsonName(v), v.getMessage()))
                .filter(f -> f.field() != null)   // a null body has no property node
                .sorted(Comparator.comparing(ApiError.FieldError::field))
                .toList();
        String message = fields.isEmpty() ? "request body is required" : "invalid request";
        return json(400, new ApiError("validation", message, fields));
    }

    static String jsonName(ConstraintViolation<?> v) {
        for (Path.Node node : v.getPropertyPath()) {
            if (node.getKind() == ElementKind.PROPERTY) {
                return node.getName();
            }
        }
        return null;
    }

    @ServerExceptionMapper
    public Response authenticationFailed(AuthenticationFailedException e) {
        return json(401, ApiError.of("unauthorized", "authentication required"));
    }

    @ServerExceptionMapper
    public Response unauthorized(UnauthorizedException e) {
        return Response.status(401)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiError.of("unauthorized", "authentication required"))
                .build();
    }

    @ServerExceptionMapper
    public Response unexpected(Throwable t) {
        LOG.error("unexpected error", t);
        return json(500, ApiError.of("internal", "unexpected error"));
    }

    static String codeFor(int status) {
        if (status >= 500) {
            return "internal";
        }
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
