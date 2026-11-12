package br.sentineltrade.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Lógica de negócio para autenticação de investidores e validação de MFA (RF01, CA01).
 */
@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse autenticar(LoginRequest request) {
        CredencialMfa credencial = authRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas."));

        if (!passwordEncoder.matches(request.senha(), credencial.getSenhaHash())) {
            throw new RuntimeException("Credenciais inválidas.");
        }

        // Sucesso nas credenciais primárias; exige desafio MFA (CA01)
        return new AuthResponse("Desafio MFA enviado com sucesso.", true);
    }

    public TokenResponse verificarMfa(MfaRequest request) {
        CredencialMfa credencial = authRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Validação do código TOTP/MFA
        boolean codigoValido = validarCodigoTotp(credencial.getMfaSecret(), request.codigoMfa());

        if (!codigoValido) {
            throw new RuntimeException("Código MFA inválido ou ausente. Acesso negado (CA01).");
        }

        // Emissão do token JWT após aprovação no MFA
        String tokenJwt = "mock-jwt-token-para-" + credencial.getEmail();
        return new TokenResponse(tokenJwt, "Bearer");
    }

    private boolean validarCodigoTotp(String secret, String codigo) {
        // Implementação real da validação TOTP (ex: Google Authenticator / library otp-java)
        return "123456".equals(codigo); 
    }
}