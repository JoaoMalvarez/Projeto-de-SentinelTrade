package br.sentineltrade.bff.api;

import br.sentineltrade.bff.api.Dtos.CadastroMfaRequest;
import br.sentineltrade.bff.api.Dtos.CadastroRequest;
import br.sentineltrade.bff.api.Dtos.EsqueciSenhaRequest;
import br.sentineltrade.bff.api.Dtos.LoginRequest;
import br.sentineltrade.bff.api.Dtos.MfaRequest;
import br.sentineltrade.bff.backend.BackendClient;
import br.sentineltrade.bff.comum.BffException;
import br.sentineltrade.bff.sessao.Autenticacao;
import br.sentineltrade.bff.sessao.Sessao;
import br.sentineltrade.bff.sessao.SessaoStore;
import br.sentineltrade.bff.sessao.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Autenticacao (RF01). O BFF nao valida senha nem MFA: repassa ao backend.
 * O que o BFF faz e GUARDAR A SESSAO: o JWT do backend fica no BFF e o navegador
 * recebe so um token opaco (cookie httpOnly + campo "token" para o Bearer).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String MSG_ESQUECI = "Se o e-mail estiver cadastrado, você receberá as instruções.";
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final BackendClient backend;
    private final SessaoStore sessoes;
    private final Autenticacao auth;

    public AuthController(BackendClient backend, SessaoStore sessoes, Autenticacao auth) {
        this.backend = backend;
        this.sessoes = sessoes;
        this.auth = auth;
    }

    /** Etapa 1: e-mail e senha -> {mfaToken}. */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest corpo, HttpServletRequest req) {
        Object resposta = backend.post("/api/auth/login", corpo, null, req).corpo();
        return Map.of("mfaToken", texto(resposta, "mfaToken"));
    }

    /** Etapa 2: codigo TOTP -> cria a sessao e devolve {token, usuario}. */
    @PostMapping("/mfa")
    public ResponseEntity<Map<String, Object>> mfa(@Valid @RequestBody MfaRequest corpo, HttpServletRequest req) {
        Object resposta = backend.post("/api/auth/mfa", corpo, null, req).corpo();
        String jwtBackend = texto(resposta, "token");
        Usuario usuario = usuarioDe(resposta);

        Sessao sessao = sessoes.criar(jwtBackend, usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, auth.cookieDeSessao(sessao))
                .body(Map.of("token", sessao.id(), "usuario", usuario));
    }

    @PostMapping("/cadastro")
    public ResponseEntity<Object> cadastro(@Valid @RequestBody CadastroRequest corpo, HttpServletRequest req) {
        return ResponseEntity.status(201).body(backend.post("/api/auth/cadastro", corpo, null, req).corpo());
    }

    @PostMapping("/cadastro/mfa")
    public ResponseEntity<Object> cadastroMfa(@Valid @RequestBody CadastroMfaRequest corpo, HttpServletRequest req) {
        return ResponseEntity.status(201).body(backend.post("/api/auth/cadastro/mfa", corpo, null, req).corpo());
    }

    /** Sempre a mesma resposta, exista ou nao o e-mail (nao revela quem e cliente). */
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Map<String, String>> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest corpo,
                                                            HttpServletRequest req) {
        try {
            backend.post("/api/auth/esqueci-senha", corpo, null, req);
        } catch (BffException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 429) {
                throw e;
            }
        }
        return ResponseEntity.status(202).body(Map.of("mensagem", MSG_ESQUECI));
    }

    /** Invalida o token no backend, apaga a sessao do BFF e o cookie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        Optional<Sessao> sessao = auth.atual(req);
        sessao.ifPresent(s -> {
            try {
                backend.post("/api/auth/logout", null, s, req);
            } catch (BffException e) {
                log.debug("Backend nao confirmou o logout: {}", e.getMessage());
            }
            sessoes.remover(s.id());
        });
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, auth.cookieRemovido()).build();
    }

    private static String texto(Object resposta, String campo) {
        if (resposta instanceof Map<?, ?> mapa && mapa.get(campo) instanceof String valor && !valor.isBlank()) {
            return valor;
        }
        throw new BffException(502, BackendClient.MSG_INDISPONIVEL);
    }

    private static Usuario usuarioDe(Object resposta) {
        if (resposta instanceof Map<?, ?> mapa && mapa.get("usuario") instanceof Map<?, ?> u) {
            Object id = u.get("id");
            Object perfil = u.get("perfil");
            if (id != null && perfil instanceof String p && Usuario.PERFIS.contains(p)) {
                return new Usuario(String.valueOf(id), textoOuVazio(u.get("nome")), textoOuVazio(u.get("email")), p);
            }
        }
        throw new BffException(502, BackendClient.MSG_INDISPONIVEL);
    }

    private static String textoOuVazio(Object valor) {
        return valor instanceof String s ? s : "";
    }
}
