package br.sentineltrade.bff.api;

import br.sentineltrade.bff.api.Dtos.LimitesRequest;
import br.sentineltrade.bff.api.Dtos.MercadoRequest;
import br.sentineltrade.bff.api.Dtos.ModoSeguroRequest;
import br.sentineltrade.bff.backend.BackendClient;
import br.sentineltrade.bff.comum.Validacoes;
import br.sentineltrade.bff.sessao.Autenticacao;
import br.sentineltrade.bff.sessao.Sessao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administracao: limites, mercado por ativo e modo seguro. So ADMIN. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String ADMIN = "ADMIN";

    private final BackendClient backend;
    private final Autenticacao auth;

    public AdminController(BackendClient backend, Autenticacao auth) {
        this.backend = backend;
        this.auth = auth;
    }

    @GetMapping("/estado")
    public Object estado(HttpServletRequest req) {
        Sessao s = auth.exigir(req, ADMIN);
        return backend.get("/api/admin/estado", s, req).corpo();
    }

    @PutMapping("/investidores/{id}/limites")
    public ResponseEntity<Void> limites(@PathVariable String id, @Valid @RequestBody LimitesRequest corpo,
                                        HttpServletRequest req) {
        Sessao s = auth.exigir(req, ADMIN);
        backend.put("/api/admin/investidores/" + Validacoes.id(id) + "/limites", corpo, s, req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/ativos/{ticker}/mercado")
    public ResponseEntity<Void> mercado(@PathVariable String ticker, @Valid @RequestBody MercadoRequest corpo,
                                        HttpServletRequest req) {
        Sessao s = auth.exigir(req, ADMIN);
        backend.put("/api/admin/ativos/" + Validacoes.ticker(ticker) + "/mercado", corpo, s, req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/modo-seguro")
    public ResponseEntity<Void> modoSeguro(@Valid @RequestBody ModoSeguroRequest corpo, HttpServletRequest req) {
        Sessao s = auth.exigir(req, ADMIN);
        backend.put("/api/admin/modo-seguro", corpo, s, req);
        return ResponseEntity.noContent().build();
    }
}
