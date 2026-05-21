package rs.skola.platforma.zamene.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OdbijZamenuRequest(
        @NotBlank @Size(max = 500) String razlog
) {}
