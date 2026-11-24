package br.sentineltrade.bff.comum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidacoesTest {

    private static int status(Runnable r) {
        return assertThrows(BffException.class, r::run).getStatus();
    }

    @Test
    void ticker() {
        assertEquals("PETR4", Validacoes.ticker("petr4"));
        assertEquals("BOVA11", Validacoes.ticker(" BOVA11 "));
        assertEquals(400, status(() -> Validacoes.ticker("PE")));
        assertEquals(400, status(() -> Validacoes.ticker("../admin")));
        assertEquals(400, status(() -> Validacoes.ticker(null)));
    }

    @Test
    void id() {
        assertEquals("ORD-0001", Validacoes.id("ORD-0001"));
        assertEquals("3f2a7c1e-9d2b-4b7e-8a51-0c6f2e1d9b44", Validacoes.id("3f2a7c1e-9d2b-4b7e-8a51-0c6f2e1d9b44"));
        assertEquals(400, status(() -> Validacoes.id("1/../../admin")));
        assertEquals(400, status(() -> Validacoes.id("1?x=2")));
    }

    @Test
    void chaveIdempotencia() {
        assertEquals("3f2a7c1e-9d2b-4b7e-8a51-0c6f2e1d9b44",
                Validacoes.chaveIdempotencia("3f2a7c1e-9d2b-4b7e-8a51-0c6f2e1d9b44"));
        assertEquals(400, status(() -> Validacoes.chaveIdempotencia(null)));
        assertEquals(400, status(() -> Validacoes.chaveIdempotencia("curta")));
        assertEquals(400, status(() -> Validacoes.chaveIdempotencia("a".repeat(101))));
    }

    @Test
    void filtros() {
        assertNull(Validacoes.opcional("", Validacoes.ABAS, "aba"));
        assertEquals("historico", Validacoes.opcional("historico", Validacoes.ABAS, "aba"));
        assertEquals(400, status(() -> Validacoes.opcional("tudo", Validacoes.ABAS, "aba")));
        assertEquals("LOGIN_OK", Validacoes.eventoOpcional("LOGIN_OK"));
        assertEquals(400, status(() -> Validacoes.eventoOpcional("login ok; drop")));
    }

    @Test
    void textoBusca() {
        assertNull(Validacoes.textoBusca("   ", 100));
        assertEquals("ana", Validacoes.textoBusca(" ana\n\r ", 100));
        assertEquals(400, status(() -> Validacoes.textoBusca("x".repeat(101), 100)));
    }
}
