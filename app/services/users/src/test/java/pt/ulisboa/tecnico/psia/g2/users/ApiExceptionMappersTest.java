package pt.ulisboa.tecnico.psia.g2.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApiExceptionMappersTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void codeForMapsStatusesToFixedCodes() {
        assertEquals("validation", ApiExceptionMappers.codeFor(400));
        assertEquals("unauthorized", ApiExceptionMappers.codeFor(401));
        assertEquals("not_found", ApiExceptionMappers.codeFor(404));
        assertEquals("validation", ApiExceptionMappers.codeFor(405));
        assertEquals("conflict", ApiExceptionMappers.codeFor(409));
        assertEquals("validation", ApiExceptionMappers.codeFor(415));
        assertEquals("internal", ApiExceptionMappers.codeFor(500));
        assertEquals("internal", ApiExceptionMappers.codeFor(503));
    }

    @Test
    void usesJsonPropertyNameNotJavaPath() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                VALIDATOR.validate(new RegisterRequest("a", "short", null, null));

        Set<String> names = violations.stream().map(ApiExceptionMappers::jsonName).collect(Collectors.toSet());

        assertEquals(Set.of("username", "password"), names);
    }

    @Test
    void containerElementReportsCollectionProperty() {
        Set<Genre> genres = new HashSet<>();
        genres.add(null);

        Set<ConstraintViolation<RegisterRequest>> violations =
                VALIDATOR.validate(new RegisterRequest("abc", "password1", null, genres));

        assertFalse(violations.isEmpty());
        for (ConstraintViolation<RegisterRequest> v : violations) {
            assertEquals("favoriteGenres", ApiExceptionMappers.jsonName(v));
        }
    }
}
