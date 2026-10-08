package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull
        @Pattern(regexp = "[A-Za-z0-9_-]{3,32}",
                message = "Must have 3-32 characters: letters, digits, '_' or '-'")
        String username,

        // bcrypt only uses the first 72 bytes. @Size counts characters, @MaxUtf8Bytes counts
        // UTF-8 bytes (non-ASCII characters take more than one); both report under "password".
        @NotNull
        @Size(min = 8, max = 72)
        @MaxUtf8Bytes(72)
        String password) {
}
