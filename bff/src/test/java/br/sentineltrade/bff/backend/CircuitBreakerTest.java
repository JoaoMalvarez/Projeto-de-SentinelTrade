package br.sentineltrade.bff.backend;

import br.sentineltrade.bff.backend.CircuitBreaker.Estado;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitBreakerTest {

    private final AtomicLong agora = new AtomicLong(0);
    private final CircuitBreaker cb = new CircuitBreaker(3, Duration.ofSeconds(30), agora::get);

    private void falhar(int vezes) {
        for (int i = 0; i < vezes; i++) {
            assertTrue(cb.permitir());
            cb.registrarFalha();
        }
    }

    @Test
    void abreDepoisDeNFalhasSeguidas() {
        falhar(2);
        assertEquals(Estado.FECHADO, cb.estado());
        falhar(1);
        assertEquals(Estado.ABERTO, cb.estado());
        assertFalse(cb.permitir());
    }

    @Test
    void sucessoZeraAContagem() {
        falhar(2);
        cb.registrarSucesso();
        falhar(2);
        assertEquals(Estado.FECHADO, cb.estado());
    }

    @Test
    void depoisDoTempoDeixaPassarUmTeste() {
        falhar(3);
        agora.addAndGet(29_999);
        assertFalse(cb.permitir());
        agora.addAndGet(1);
        assertTrue(cb.permitir());             // chamada de teste
        assertEquals(Estado.MEIO_ABERTO, cb.estado());
        assertFalse(cb.permitir());            // so uma por vez
    }

    @Test
    void testeComSucessoFecha() {
        falhar(3);
        agora.addAndGet(30_000);
        assertTrue(cb.permitir());
        cb.registrarSucesso();
        assertEquals(Estado.FECHADO, cb.estado());
        assertTrue(cb.permitir());
    }

    @Test
    void testeComFalhaReabre() {
        falhar(3);
        agora.addAndGet(30_000);
        assertTrue(cb.permitir());
        cb.registrarFalha();
        assertEquals(Estado.ABERTO, cb.estado());
        assertFalse(cb.permitir());
    }
}
