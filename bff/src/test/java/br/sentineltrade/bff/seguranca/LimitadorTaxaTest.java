package br.sentineltrade.bff.seguranca;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitadorTaxaTest {

    private final AtomicLong agora = new AtomicLong(1_000_000);
    private final LimitadorTaxa limitador = new LimitadorTaxa(Duration.ofMinutes(1), agora::get);

    @Test
    void permiteAteOLimiteEBloqueiaODepois() {
        for (int i = 0; i < 5; i++) {
            assertTrue(limitador.tentar("1.2.3.4|auth", 5).permitido());
        }
        LimitadorTaxa.Resultado r = limitador.tentar("1.2.3.4|auth", 5);
        assertFalse(r.permitido());
        assertEquals(60, r.aguardarSegundos());
    }

    @Test
    void novaJanelaLiberaDeNovo() {
        for (int i = 0; i < 3; i++) {
            limitador.tentar("ip", 2);
        }
        assertFalse(limitador.tentar("ip", 2).permitido());
        agora.addAndGet(60_000);
        assertTrue(limitador.tentar("ip", 2).permitido());
    }

    @Test
    void chavesSaoIndependentes() {
        assertTrue(limitador.tentar("a", 1).permitido());
        assertFalse(limitador.tentar("a", 1).permitido());
        assertTrue(limitador.tentar("b", 1).permitido());
    }

    @Test
    void tempoDeEsperaDiminuiComOTempo() {
        limitador.tentar("ip", 1);
        agora.addAndGet(45_000);
        LimitadorTaxa.Resultado r = limitador.tentar("ip", 1);
        assertFalse(r.permitido());
        assertEquals(15, r.aguardarSegundos());
    }
}
