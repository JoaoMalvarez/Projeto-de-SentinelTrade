package br.sentineltrade.bff.seguranca;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Rate limit por janela fixa: no maximo N requisicoes por chave (ex.: IP) a cada janela.
 * Protege principalmente o login contra tentativa e erro de senha (forca bruta).
 */
public final class LimitadorTaxa {

    public record Resultado(boolean permitido, long aguardarSegundos) {}

    private record Janela(long inicio, int contagem) {}

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final long janelaMs;
    private final LongSupplier relogioMs;

    public LimitadorTaxa(Duration janela, LongSupplier relogioMs) {
        this.janelaMs = janela.toMillis();
        this.relogioMs = relogioMs;
    }

    public Resultado tentar(String chave, int limite) {
        long agora = relogioMs.getAsLong();
        Janela janela = janelas.compute(chave, (k, atual) ->
                (atual == null || agora - atual.inicio() >= janelaMs)
                        ? new Janela(agora, 1)
                        : new Janela(atual.inicio(), atual.contagem() + 1));
        if (janela.contagem() <= limite) {
            return new Resultado(true, 0);
        }
        long restanteMs = janela.inicio() + janelaMs - agora;
        return new Resultado(false, Math.max(1, (restanteMs + 999) / 1000));
    }

    public void limparAntigas() {
        long agora = relogioMs.getAsLong();
        janelas.values().removeIf(j -> agora - j.inicio() >= janelaMs);
    }
}
