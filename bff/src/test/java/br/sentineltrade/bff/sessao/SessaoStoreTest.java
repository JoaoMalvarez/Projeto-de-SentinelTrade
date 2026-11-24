package br.sentineltrade.bff.sessao;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessaoStoreTest {

    /** Relogio que o teste controla. */
    static final class RelogioFalso extends Clock {
        private Instant agora = Instant.parse("2026-11-23T12:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final RelogioFalso relogio = new RelogioFalso();
    private final SessaoStore store = new SessaoStore(Duration.ofMinutes(30), Duration.ofHours(8), relogio);
    private final Usuario ana = new Usuario("u1", "Ana", "ana@orion.test", "INVESTIDOR");

    @Test
    void criaEBuscaSessao() {
        Sessao s = store.criar("jwt-backend", ana);
        assertTrue(store.buscar(s.id()).isPresent());
        assertEquals("jwt-backend", store.buscar(s.id()).get().tokenBackend());
    }

    @Test
    void idsSaoLongosEUnicos() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String id = store.criar("jwt", ana).id();
            assertTrue(id.length() >= 40);
            ids.add(id);
        }
        assertEquals(200, ids.size());
    }

    @Test
    void tokenDesconhecidoOuInvalidoNaoAcha() {
        assertFalse(store.buscar("nao-existe").isPresent());
        assertFalse(store.buscar(null).isPresent());
        assertFalse(store.buscar("").isPresent());
        assertFalse(store.buscar("x".repeat(500)).isPresent());
    }

    @Test
    void expiraPorInatividade() {
        Sessao s = store.criar("jwt", ana);
        relogio.avancar(Duration.ofMinutes(29));
        assertTrue(store.buscar(s.id()).isPresent()); // uso renova o prazo
        relogio.avancar(Duration.ofMinutes(29));
        assertTrue(store.buscar(s.id()).isPresent());
        relogio.avancar(Duration.ofMinutes(30));
        assertFalse(store.buscar(s.id()).isPresent());
    }

    @Test
    void expiraPelaDuracaoMaximaMesmoEmUso() {
        Sessao s = store.criar("jwt", ana);
        for (int i = 0; i < 16; i++) { // 16 x 29 min = 7h44
            relogio.avancar(Duration.ofMinutes(29));
            assertTrue(store.buscar(s.id()).isPresent());
        }
        relogio.avancar(Duration.ofMinutes(20)); // passa de 8h
        assertFalse(store.buscar(s.id()).isPresent());
    }

    @Test
    void ativaNaoRenovaOPrazo() {
        Sessao s = store.criar("jwt", ana);
        relogio.avancar(Duration.ofMinutes(20));
        assertTrue(store.ativa(s.id()));
        relogio.avancar(Duration.ofMinutes(15)); // 35 min sem buscar()
        assertFalse(store.ativa(s.id()));
    }

    @Test
    void removerEncerraASessao() {
        Sessao s = store.criar("jwt", ana);
        store.remover(s.id());
        assertFalse(store.buscar(s.id()).isPresent());
    }

    @Test
    void limpezaRemoveSoAsExpiradas() {
        store.criar("jwt", ana);
        relogio.avancar(Duration.ofMinutes(40));
        Sessao nova = store.criar("jwt", ana);
        assertEquals(1, store.limparExpiradas());
        assertEquals(1, store.quantidade());
        assertTrue(store.buscar(nova.id()).isPresent());
    }

    @Test
    void toStringNaoVazaToken() {
        Sessao s = store.criar("segredo-jwt", ana);
        assertFalse(s.toString().contains("segredo-jwt"));
        assertFalse(s.toString().contains(s.id()));
        assertNotEquals(s.id(), s.tokenBackend());
    }
}
