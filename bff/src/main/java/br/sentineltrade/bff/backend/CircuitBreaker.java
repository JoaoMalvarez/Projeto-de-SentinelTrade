package br.sentineltrade.bff.backend;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Circuit breaker simples para as chamadas ao backend.
 *
 *  FECHADO     -> tudo normal. Apos N falhas seguidas, ABRE.
 *  ABERTO      -> recusa na hora (503), sem martelar um backend fora do ar.
 *                 Depois do tempo de espera, passa a MEIO_ABERTO.
 *  MEIO_ABERTO -> deixa passar UMA chamada de teste.
 *                 Sucesso fecha o circuito; falha abre de novo.
 *
 * Contam como falha: erro de rede, timeout e resposta 5xx. Erros 4xx sao normais.
 */
public final class CircuitBreaker {

    public enum Estado { FECHADO, ABERTO, MEIO_ABERTO }

    private final int falhasParaAbrir;
    private final long tempoAbertoMs;
    private final LongSupplier relogioMs;

    private Estado estado = Estado.FECHADO;
    private int falhasSeguidas;
    private long abertoDesde;
    private boolean testeEmAndamento;

    public CircuitBreaker(int falhasParaAbrir, Duration tempoAberto, LongSupplier relogioMs) {
        this.falhasParaAbrir = falhasParaAbrir;
        this.tempoAbertoMs = tempoAberto.toMillis();
        this.relogioMs = relogioMs;
    }

    /** Pergunta se a chamada pode ser feita. Toda chamada permitida DEVE registrar sucesso ou falha. */
    public synchronized boolean permitir() {
        switch (estado) {
            case FECHADO:
                return true;
            case ABERTO:
                if (relogioMs.getAsLong() - abertoDesde >= tempoAbertoMs) {
                    estado = Estado.MEIO_ABERTO;
                    testeEmAndamento = true;
                    return true;
                }
                return false;
            case MEIO_ABERTO:
            default:
                if (!testeEmAndamento) {
                    testeEmAndamento = true;
                    return true;
                }
                return false;
        }
    }

    public synchronized void registrarSucesso() {
        estado = Estado.FECHADO;
        falhasSeguidas = 0;
        testeEmAndamento = false;
    }

    public synchronized void registrarFalha() {
        if (estado == Estado.MEIO_ABERTO) {
            abrir();
            return;
        }
        falhasSeguidas++;
        if (falhasSeguidas >= falhasParaAbrir) {
            abrir();
        }
    }

    public synchronized Estado estado() {
        return estado;
    }

    private void abrir() {
        estado = Estado.ABERTO;
        abertoDesde = relogioMs.getAsLong();
        falhasSeguidas = 0;
        testeEmAndamento = false;
    }
}
