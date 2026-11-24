package br.sentineltrade.bff.sessao;

import br.sentineltrade.bff.comum.BffException;
import br.sentineltrade.bff.config.BffProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Descobre quem esta chamando e confere o perfil (primeira barreira; o backend revalida).
 *
 * O navegador se identifica de duas formas (as duas apontam para a mesma sessao):
 *  1. Cookie ST_SESSAO (httpOnly, SameSite=Strict) - o JavaScript nao consegue ler. E o que
 *     o stream de cotacoes (EventSource) usa, porque EventSource nao envia cabecalhos.
 *  2. Authorization: Bearer <token> - como o frontend atual ja faz.
 */
@Component
public class Autenticacao {

    public static final String COOKIE = "ST_SESSAO";

    private final SessaoStore sessoes;
    private final BffProperties props;

    public Autenticacao(SessaoStore sessoes, BffProperties props) {
        this.sessoes = sessoes;
        this.props = props;
    }

    /** Exige sessao valida (senao 401) e, se informado, um dos perfis (senao 403). */
    public Sessao exigir(HttpServletRequest req, String... perfis) {
        Sessao sessao = atual(req)
                .orElseThrow(() -> new BffException(401, "Sessão expirada. Faça login novamente."));
        if (perfis.length > 0 && !sessao.usuario().temPerfil(perfis)) {
            throw new BffException(403, "Você não tem permissão para acessar este recurso.");
        }
        return sessao;
    }

    /** Sessao atual, se houver (sem erro). */
    public Optional<Sessao> atual(HttpServletRequest req) {
        Optional<Sessao> pelaCabecalho = tokenBearer(req).flatMap(sessoes::buscar);
        if (pelaCabecalho.isPresent()) {
            return pelaCabecalho;
        }
        return tokenCookie(req).flatMap(sessoes::buscar);
    }

    public String cookieDeSessao(Sessao sessao) {
        return cookie(sessao.id(), sessoes.duracaoMaxima());
    }

    public String cookieRemovido() {
        return cookie("", Duration.ZERO);
    }

    private String cookie(String valor, Duration validade) {
        return ResponseCookie.from(COOKIE, valor)
                .httpOnly(true)
                .secure(props.cookieSeguro())
                .sameSite("Strict")
                .path("/api")
                .maxAge(validade)
                .build()
                .toString();
    }

    private static Optional<String> tokenBearer(HttpServletRequest req) {
        String cabecalho = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = cabecalho.substring(7).trim();
            return token.isEmpty() ? Optional.empty() : Optional.of(token);
        }
        return Optional.empty();
    }

    private static Optional<String> tokenCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie c : cookies) {
            if (COOKIE.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }
}
