package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        String username,

        @NotBlank
        String password) {
}
