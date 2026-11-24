package br.sentineltrade.bff.backend;

import br.sentineltrade.bff.comum.BffException;
import br.sentineltrade.bff.comum.RedeUtil;
import br.sentineltrade.bff.comum.Validacoes;
import br.sentineltrade.bff.config.BffProperties;
import br.sentineltrade.bff.filtros.CorrelationIdFilter;
import br.sentineltrade.bff.sessao.Sessao;
import br.sentineltrade.bff.sessao.SessaoStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unico ponto de contato do BFF com o backend.
 *
 * Em cada chamada:
 *  - repassa o JWT do usuario (que so o BFF conhece), o X-Correlation-Id, o IP e o
 *    user-agent do cliente (para a auditoria) e a chave interna X-Bff-Key;
 *  - aplica timeout (configurado no RestClient) e circuit breaker;
 *  - traduz os erros do backend para os codigos que o frontend trata
 *    (400, 401, 403, 404, 409, 422, 503), sem vazar detalhes internos.
 */
@Component
public class BackendClient {

    /** Resposta de sucesso do backend: status + corpo ja convertido (Map, List, numero, texto ou null). */
    public record Resposta(int status, Object corpo) {}

    private record Bruta(int status, byte[] corpo) {}

    public static final String MSG_INDISPONIVEL = "Serviço temporariamente indisponível. Tente novamente em instantes.";
    private static final String MSG_MODO_SEGURO = "Sistema em modo seguro: operação indisponível no momento.";
    private static final Set<Integer> REPASSA_STATUS = Set.of(400, 403, 404, 409, 422, 429);
    private static final int MAX_RESPOSTA_BYTES = 2 * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(BackendClient.class);

    private final RestClient rest;
    private final ObjectMapper json;
    private final CircuitBreaker circuito;
    private final SessaoStore sessoes;
    private final BffProperties props;

    public BackendClient(RestClient rest, ObjectMapper json, CircuitBreaker circuito,
                         SessaoStore sessoes, BffProperties props) {
        this.rest = rest;
        this.json = json;
        this.circuito = circuito;
        this.sessoes = sessoes;
        this.props = props;
    }

    public Resposta get(String caminho, Sessao sessao, HttpServletRequest origem) {
        return chamar(HttpMethod.GET, caminho, null, sessao, origem, Map.of());
    }

    public Resposta post(String caminho, Object corpo, Sessao sessao, HttpServletRequest origem) {
        return chamar(HttpMethod.POST, caminho, corpo, sessao, origem, Map.of());
    }

    public Resposta put(String caminho, Object corpo, Sessao sessao, HttpServletRequest origem) {
        return chamar(HttpMethod.PUT, caminho, corpo, sessao, origem, Map.of());
    }

    public Resposta patch(String caminho, Sessao sessao, HttpServletRequest origem) {
        return chamar(HttpMethod.PATCH, caminho, null, sessao, origem, Map.of());
    }

    public Resposta delete(String caminho, Sessao sessao, HttpServletRequest origem) {
        return chamar(HttpMethod.DELETE, caminho, null, sessao, origem, Map.of());
    }

    /**
     * Faz a chamada ao backend.
     *
     * @param caminho caminho ja validado, comecando com "/" (ex.: "/api/ordens")
     * @param sessao  sessao do usuario (null para rotas publicas, como login)
     * @param origem  requisicao original do navegador (null em tarefas agendadas)
     * @param extras  cabecalhos extras repassados sem alteracao (ex.: Idempotency-Key)
     */
    public Resposta chamar(HttpMethod metodo, String caminho, Object corpo, Sessao sessao,
                           HttpServletRequest origem, Map<String, String> extras) {
        if (!circuito.permitir()) {
            throw new BffException(503, MSG_INDISPONIVEL);
        }

        Bruta bruta;
        try {
            RestClient.RequestBodySpec pedido = rest.method(metodo)
                    .uri(URI.create(props.backendUrl() + caminho))
                    .headers(h -> preencherCabecalhos(h, sessao, origem, extras, corpo != null));
            if (corpo != null) {
                pedido.body(corpo);
            }
            bruta = pedido.exchange((req, res) -> new Bruta(res.getStatusCode().value(), ler(res.getBody())));
        } catch (RuntimeException e) {
            // Erro de rede, timeout, backend fora do ar...
            circuito.registrarFalha();
            log.warn("Falha ao chamar o backend: {} {} ({})", metodo, caminho, e.getClass().getSimpleName());
            throw new BffException(503, MSG_INDISPONIVEL);
        }

        if (bruta.status() >= 500) {
            circuito.registrarFalha();
        } else {
            circuito.registrarSucesso();
        }
        return interpretar(bruta, sessao, metodo, caminho);
    }

    /** Monta um caminho com query string, ignorando parametros vazios e codificando os valores. */
    public static String caminho(String base, String... paresNomeValor) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath(base);
        for (int i = 0; i + 1 < paresNomeValor.length; i += 2) {
            String valor = paresNomeValor[i + 1];
            if (valor != null && !valor.isBlank()) {
                b.queryParam(paresNomeValor[i], valor);
            }
        }
        return b.build().encode().toUriString();
    }

    private void preencherCabecalhos(HttpHeaders h, Sessao sessao, HttpServletRequest origem,
                                     Map<String, String> extras, boolean temCorpo) {
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (temCorpo) {
            h.setContentType(MediaType.APPLICATION_JSON);
        }
        if (sessao != null) {
            h.setBearerAuth(sessao.tokenBackend());
        }
        if (props.chaveInterna() != null && !props.chaveInterna().isBlank()) {
            h.set("X-Bff-Key", props.chaveInterna());
        }
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CHAVE);
        if (correlationId != null) {
            h.set(CorrelationIdFilter.CABECALHO, correlationId);
        }
        if (origem != null) {
            h.set("X-Forwarded-For", RedeUtil.ipCliente(origem, props.confiarProxy()));
            String agente = origem.getHeader(HttpHeaders.USER_AGENT);
            if (agente != null) {
                h.set("X-Client-User-Agent", Validacoes.truncar(Validacoes.semControle(agente), 256));
            }
        }
        extras.forEach(h::set);
    }

    private Resposta interpretar(Bruta bruta, Sessao sessao, HttpMethod metodo, String caminho) {
        int status = bruta.status();
        boolean sucesso = status >= 200 && status < 300;

        Object corpo;
        try {
            corpo = bruta.corpo().length == 0 ? null : json.readValue(bruta.corpo(), Object.class);
        } catch (IOException e) {
            if (sucesso) {
                log.warn("Backend devolveu JSON invalido: {} {}", metodo, caminho);
                throw new BffException(502, MSG_INDISPONIVEL);
            }
            corpo = null;
        }

        if (sucesso) {
            return new Resposta(status, corpo);
        }

        String mensagem = mensagemDe(corpo);
        if (status == 401) {
            // JWT do backend expirou ou foi invalidado: encerra tambem a sessao do BFF.
            if (sessao != null) {
                sessoes.remover(sessao.id());
            }
            throw new BffException(401, mensagem != null ? mensagem : "Sessão expirada. Faça login novamente.");
        }
        if (status == 503) {
            throw new BffException(503, mensagem != null ? mensagem : MSG_MODO_SEGURO);
        }
        if (REPASSA_STATUS.contains(status)) {
            throw new BffException(status, mensagem != null ? mensagem : mensagemPadrao(status));
        }
        if (status >= 400 && status < 500) {
            throw new BffException(400, mensagem != null ? mensagem : "Requisição inválida.");
        }
        log.warn("Backend respondeu {} em {} {}", status, metodo, caminho);
        throw new BffException(502, MSG_INDISPONIVEL);
    }

    /** O backend pode responder {"message": ...} (contrato) ou {"mensagem": ...} (codigo atual). */
    private static String mensagemDe(Object corpo) {
        if (corpo instanceof Map<?, ?> mapa) {
            Object m = mapa.get("message");
            if (!(m instanceof String)) {
                m = mapa.get("mensagem");
            }
            if (m instanceof String texto && !texto.isBlank()) {
                return Validacoes.truncar(Validacoes.semControle(texto).trim(), 300);
            }
        }
        return null;
    }

    private static String mensagemPadrao(int status) {
        return switch (status) {
            case 403 -> "Você não tem permissão para esta operação.";
            case 404 -> "Recurso não encontrado.";
            case 409 -> "A operação não pode ser realizada no estado atual.";
            case 422 -> "Operação recusada pelas regras de negócio.";
            case 429 -> "Muitas requisições. Aguarde um instante.";
            default -> "Dados inválidos.";
        };
    }

    private static byte[] ler(InputStream entrada) throws IOException {
        if (entrada == null) {
            return new byte[0];
        }
        byte[] bytes = entrada.readNBytes(MAX_RESPOSTA_BYTES + 1);
        if (bytes.length > MAX_RESPOSTA_BYTES) {
            throw new IOException("Resposta do backend grande demais");
        }
        return bytes;
    }
}
