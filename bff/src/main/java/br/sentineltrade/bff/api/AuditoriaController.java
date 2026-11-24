package br.sentineltrade.bff.api;

import br.sentineltrade.bff.backend.BackendClient;
import br.sentineltrade.bff.comum.Validacoes;
import br.sentineltrade.bff.sessao.Autenticacao;
import br.sentineltrade.bff.sessao.Sessao;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Auditoria (RF08): so AUDITOR e ADMIN. Somente leitura. */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private static final String[] PERFIS = {"AUDITOR", "ADMIN"};

    private final BackendClient backend;
    private final Autenticacao auth;

    public AuditoriaController(BackendClient backend, Autenticacao auth) {
        this.backend = backend;
        this.auth = auth;
    }

    @GetMapping
    public Object listar(@RequestParam(required = false) String evento,
                         @RequestParam(required = false) String q,
                         HttpServletRequest req) {
        Sessao s = auth.exigir(req, PERFIS);
        String caminho = BackendClient.caminho("/api/auditoria",
                "evento", Validacoes.eventoOpcional(evento),
                "q", Validacoes.textoBusca(q, 100));
        return backend.get(caminho, s, req).corpo();
    }

    @GetMapping("/integridade")
    public Object integridade(HttpServletRequest req) {
        Sessao s = auth.exigir(req, PERFIS);
        return backend.get("/api/auditoria/integridade", s, req).corpo();
    }
}
