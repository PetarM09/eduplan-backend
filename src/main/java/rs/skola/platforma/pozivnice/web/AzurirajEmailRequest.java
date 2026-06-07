package rs.skola.platforma.pozivnice.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AzurirajEmailRequest(
        @NotBlank @Email String email
) {}
