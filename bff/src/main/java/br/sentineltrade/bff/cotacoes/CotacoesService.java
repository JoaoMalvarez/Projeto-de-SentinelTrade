package br.sentineltrade.bff.cotacoes;

import br.sentineltrade.bff.backend.BackendClient;
import br.sentineltrade.bff.comum.BffException;
import br.sentineltrade.bff.config.BffProperties;
import br.sentineltrade.bff.filtros.CorrelationIdFilter;
import br.sentineltrade.bff.sessao.Sessao;
import br.sentineltrade.bff.sessao.SessaoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cotacoes em tempo quase real (fan-out por SSE).
 *
 * O BFF consulta o backend UMA vez a cada intervalo (so quando ha alguem conectado)
 * e reenvia a lista atualizada para TODOS os navegadores conectados.
 * Assim, 500 navegadores abertos nao viram 500 consultas por intervalo no backend.
 */
@Service
public class CotacoesService {

    private record Inscricao(SseEmitter emissor, String sessaoId) {}

    private static final Logger log = LoggerFactory.getLogger(CotacoesService.class);

    private final List<Inscricao> inscricoes = new CopyOnWriteArrayList<>();
    private final BackendClient backend;
    private final SessaoStore sessoes;
    private final BffProperties props;
    private volatile Object ultimaLista;

    public CotacoesService(BackendClient backend, SessaoStore sessoes, BffProperties props) {
        this.backend = backend;
        this.sessoes = sessoes;
        this.props = props;
    }

    public SseEmitter inscrever(Sessao sessao) {
        if (inscricoes.size() >= props.cotacoes().maxConexoes()) {
            throw new BffException(503, "Muitas conexões no momento. Tente novamente em instantes.");
        }
        SseEmitter emissor = new SseEmitter(props.cotacoes().timeoutConexao().toMillis());
        Inscricao inscricao = new Inscricao(emissor, sessao.id());
        emissor.onCompletion(() -> inscricoes.remove(inscricao));
        emissor.onTimeout(() -> {
            inscricoes.remove(inscricao);
            emissor.complete(); // o EventSource do navegador reconecta sozinho
        });
        emissor.onError(erro -> inscricoes.remove(inscricao));
        inscricoes.add(inscricao);

        Object atual = ultimaLista;
        if (atual != null) { // quem acabou de conectar ja recebe a ultima lista conhecida
            enviar(inscricao, atual);
        }
        return emissor;
    }

    @Scheduled(fixedDelayString = "${bff.cotacoes.intervalo-ms}")
    public void atualizar() {
        if (inscricoes.isEmpty()) {
            return;
        }
        MDC.put(CorrelationIdFilter.MDC_CHAVE, UUID.randomUUID().toString());
        try {
            Object lista = backend.get("/internal/cotacoes", null, null).corpo();
            if (!(lista instanceof List)) {
                log.warn("Backend devolveu cotacoes em formato inesperado");
                return;
            }
            ultimaLista = lista;
            for (Inscricao inscricao : inscricoes) {
                if (!sessoes.ativa(inscricao.sessaoId())) { // fez logout ou expirou: encerra o stream
                    inscricoes.remove(inscricao);
                    inscricao.emissor().complete();
                    continue;
                }
                enviar(inscricao, lista);
            }
        } catch (BffException e) {
            log.warn("Nao foi possivel atualizar as cotacoes: {}", e.getMessage());
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_CHAVE);
        }
    }

    public int conectados() {
        return inscricoes.size();
    }

    private void enviar(Inscricao inscricao, Object lista) {
        try {
            inscricao.emissor().send(SseEmitter.event().data(lista, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            inscricoes.remove(inscricao); // navegador fechou a aba
        }
    }
}
