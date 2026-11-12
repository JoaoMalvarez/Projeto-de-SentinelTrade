package br.sentineltrade.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de autenticacao (login, verificacao do codigo MFA). RF01.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> autenticar(@RequestBody LoginRequest request) {
        AuthResponse response = authService.autenticar(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verificar")
    public ResponseEntity<TokenResponse> verificarMfa(@RequestBody MfaRequest request) {
        TokenResponse response = authService.verificarMfa(request);
        return ResponseEntity.ok(response);
    }
}

// DTOs auxiliares
record LoginRequest(String email, String senha) {}
record MfaRequest(String email, String codigoMfa) {}
record AuthResponse(String mensagem, boolean mfaRequerido) {}
record TokenResponse(String tokenJwt, String tipo) {}