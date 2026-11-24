package br.sentineltrade.bff.api;

import br.sentineltrade.bff.api.Dtos.OrdemRequest;
import br.sentineltrade.bff.backend.BackendClient;
import br.sentineltrade.bff.backend.BackendClient.Resposta;
import br.sentineltrade.bff.comum.Validacoes;
import br.sentineltrade.bff.dashboard.DashboardMontador;
import br.sentineltrade.bff.sessao.Autenticacao;
import br.sentineltrade.bff.sessao.Sessao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Rotas do investidor: dashboard, carteira (RF02), ativos (RF03), ordens (RF04-06) e notificacoes (RF07). */
@RestController
@RequestMapping("/api")
public class InvestidorController {

    private static final String INVESTIDOR = "INVESTIDOR";

    private final BackendClient backend;
    private final Autenticacao auth;

    public InvestidorController(BackendClient backend, Autenticacao auth) {
        this.backend = backend;
        this.auth = auth;
    }

    /** Agrega carteira + ordens + ativos em uma unica resposta para a tela inicial. */
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        Object carteira = backend.get("/api/carteira", s, req).corpo();
        Object ordens = backend.get(BackendClient.caminho("/api/ordens", "aba", "todas"), s, req).corpo();
        Object ativos = backend.get("/api/ativos", s, req).corpo();
        return DashboardMontador.montar(carteira, ordens, ativos);
    }

    @GetMapping("/carteira")
    public Object carteira(HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        return backend.get("/api/carteira", s, req).corpo();
    }

    @GetMapping("/ativos")
    public Object ativos(HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        return backend.get("/api/ativos", s, req).corpo();
    }

    @GetMapping("/ativos/{ticker}")
    public Object ativo(@PathVariable String ticker, HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        return backend.get("/api/ativos/" + Validacoes.ticker(ticker), s, req).corpo();
    }

    /** Cria ordem. O Idempotency-Key e obrigatorio e vai ao backend SEM alteracao (RNF02). */
    @PostMapping("/ordens")
    public ResponseEntity<Object> criarOrdem(@RequestHeader(value = "Idempotency-Key", required = false) String chave,
                                             @Valid @RequestBody OrdemRequest corpo,
                                             HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        String chaveValida = Validacoes.chaveIdempotencia(chave);
        Resposta r = backend.chamar(HttpMethod.POST, "/api/ordens", corpo, s, req,
                Map.of("Idempotency-Key", chaveValida));
        return ResponseEntity.status(r.status()).body(r.corpo());
    }

    @GetMapping("/ordens")
    public Object ordens(@RequestParam(required = false) String aba,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String tipo,
                         HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        String caminho = BackendClient.caminho("/api/ordens",
                "aba", Validacoes.opcional(aba, Validacoes.ABAS, "aba"),
                "status", Validacoes.opcional(status, Validacoes.STATUS_ORDEM, "status"),
                "tipo", Validacoes.opcional(tipo, Validacoes.TIPOS_ORDEM, "tipo"));
        return backend.get(caminho, s, req).corpo();
    }

    @GetMapping("/ordens/{id}")
    public Object ordem(@PathVariable String id, HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        return backend.get("/api/ordens/" + Validacoes.id(id), s, req).corpo();
    }

    @DeleteMapping("/ordens/{id}")
    public Object cancelarOrdem(@PathVariable String id, HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        return backend.delete("/api/ordens/" + Validacoes.id(id), s, req).corpo();
    }

    @GetMapping("/notificacoes")
    public Object notificacoes(HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        return backend.get("/api/notificacoes", s, req).corpo();
    }

    @PatchMapping("/notificacoes/{id}/lida")
    public ResponseEntity<Void> marcarLida(@PathVariable String id, HttpServletRequest req) {
        Sessao s = auth.exigir(req, INVESTIDOR);
        backend.patch("/api/notificacoes/" + Validacoes.id(id) + "/lida", s, req);
        return ResponseEntity.noContent().build();
    }
}
