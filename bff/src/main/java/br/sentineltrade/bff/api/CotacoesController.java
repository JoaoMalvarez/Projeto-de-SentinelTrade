package br.sentineltrade.bff.api;

import br.sentineltrade.bff.cotacoes.CotacoesService;
import br.sentineltrade.bff.sessao.Autenticacao;
import br.sentineltrade.bff.sessao.Sessao;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** GET /api/cotacoes/stream: Server-Sent Events. Cada evento e a lista de ativos atualizada (RF03). */
@RestController
public class CotacoesController {

    private final CotacoesService cotacoes;
    private final Autenticacao auth;

    public CotacoesController(CotacoesService cotacoes, Autenticacao auth) {
        this.cotacoes = cotacoes;
        this.auth = auth;
    }

    @GetMapping(path = "/api/cotacoes/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest req) {
        Sessao s = auth.exigir(req, "INVESTIDOR");
        return cotacoes.inscrever(s);
    }
}
