package rs.skola.platforma.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.auth.web.LoginRequest;
import rs.skola.platforma.auth.web.RefreshRequest;
import rs.skola.platforma.auth.web.TokenPair;
import rs.skola.platforma.common.web.ApiResponse;

@Tag(name = "Auth", description = "Autentifikacija i upravljanje sesijom")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Prijava sa korisnickim imenom i lozinkom")
    public ApiResponse<TokenPair> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Obnova access tokena uz validan refresh token (rotacija)")
    public ApiResponse<TokenPair> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    @Operation(summary = "Opoziva refresh token (logout sa trenutnog uredjaja)")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshRequest req) {
        authService.logout(req == null ? null : req.refreshToken());
        return ApiResponse.ok();
    }
}
