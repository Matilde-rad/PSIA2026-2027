package pt.ulisboa.tecnico.psia.g2.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String error, String message, List<FieldError> fields) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(String error, String message) {
        return new ApiError(error, message, null);
    }
}
