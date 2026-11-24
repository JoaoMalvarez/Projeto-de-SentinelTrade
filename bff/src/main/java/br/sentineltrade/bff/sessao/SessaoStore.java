package br.sentineltrade.bff.sessao;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda as sessoes em memoria.
 *
 * Uma sessao expira por inatividade (ex.: 30 min sem uso) ou pela duracao maxima
 * (ex.: 8 h), o que vier primeiro. Ao reiniciar o BFF todos precisam logar de novo,
 * o que e aceitavel para o prototipo (para varias instancias, trocar por Redis).
 */
public final class SessaoStore {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int TAMANHO_MAXIMO_ID = 100;

    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final Duration inatividade;
    private final Duration duracaoMaxima;
    private final Clock relogio;

    public SessaoStore(Duration inatividade, Duration duracaoMaxima, Clock relogio) {
        this.inatividade = inatividade;
        this.duracaoMaxima = duracaoMaxima;
        this.relogio = relogio;
    }

    /** Cria uma sessao nova com um id imprevisivel (256 bits aleatorios). */
    public Sessao criar(String tokenBackend, Usuario usuario) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Sessao sessao = new Sessao(id, tokenBackend, usuario, relogio.instant());
        sessoes.put(id, sessao);
        return sessao;
    }

    /** Busca a sessao e renova o prazo de inatividade. Vazio se nao existir ou tiver expirado. */
    public Optional<Sessao> buscar(String id) {
        Optional<Sessao> sessao = valida(id);
        sessao.ifPresent(s -> s.tocar(relogio.instant()));
        return sessao;
    }

    /** Diz se a sessao ainda vale, SEM renovar o prazo (usado pelo stream de cotacoes). */
    public boolean ativa(String id) {
        return valida(id).isPresent();
    }

    public void remover(String id) {
        if (id != null) {
            sessoes.remove(id);
        }
    }

    public int limparExpiradas() {
        Instant agora = relogio.instant();
        int antes = sessoes.size();
        sessoes.values().removeIf(s -> expirada(s, agora));
        return antes - sessoes.size();
    }

    public int quantidade() {
        return sessoes.size();
    }

    public Duration duracaoMaxima() {
        return duracaoMaxima;
    }

    private Optional<Sessao> valida(String id) {
        if (id == null || id.isBlank() || id.length() > TAMANHO_MAXIMO_ID) {
            return Optional.empty();
        }
        Sessao sessao = sessoes.get(id);
        if (sessao == null) {
            return Optional.empty();
        }
        if (expirada(sessao, relogio.instant())) {
            sessoes.remove(id, sessao);
            return Optional.empty();
        }
        return Optional.of(sessao);
    }

    private boolean expirada(Sessao s, Instant agora) {
        boolean parada = !agora.isBefore(s.ultimoAcesso().plus(inatividade));
        boolean velha = !agora.isBefore(s.criadaEm().plus(duracaoMaxima));
        return parada || velha;
    }
}
