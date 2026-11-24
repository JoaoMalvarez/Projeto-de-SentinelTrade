package br.sentineltrade.bff.dashboard;

import br.sentineltrade.bff.comum.BffException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardMontadorTest {

    private static Map<String, Object> ativo(String ticker, String mercado, String variacao) {
        return Map.of("ticker", ticker, "mercado", mercado, "variacao", new BigDecimal(variacao));
    }

    private static Map<String, Object> ordem(String id, String criadoEm) {
        return Map.of("id", id, "criadoEm", criadoEm);
    }

    @Test
    void somaPatrimonioComSaldosEPosicoes() {
        Object carteira = Map.of(
                "saldoDisponivel", new BigDecimal("5000"),
                "saldoBloqueado", new BigDecimal("150.10"),
                "posicoes", List.of(Map.of("valorAtual", new BigDecimal("3850.00")),
                        Map.of("valorAtual", 100)));
        Map<String, Object> d = DashboardMontador.montar(carteira, List.of(), List.of());
        assertEquals(new BigDecimal("9100.10"), d.get("patrimonio"));
        assertEquals(new BigDecimal("5000"), d.get("saldoDisponivel"));
    }

    @Test
    void ordensRecentesSaoAsCincoMaisNovas() {
        List<Object> ordens = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            ordens.add(ordem("o" + i, "2026-11-0" + i + "T10:00:00Z"));
        }
        Map<String, Object> d = DashboardMontador.montar(Map.of(), ordens, List.of());
        List<?> recentes = (List<?>) d.get("ordensRecentes");
        assertEquals(5, recentes.size());
        assertEquals("o7", ((Map<?, ?>) recentes.get(0)).get("id"));
        assertEquals("o3", ((Map<?, ?>) recentes.get(4)).get("id"));
    }

    @Test
    void destaquesSaoOsAbertosQueMaisVariaram() {
        List<Object> ativos = List.of(
                ativo("PETR4", "ABERTO", "1.05"),
                ativo("VALE3", "ABERTO", "-3.20"),
                ativo("MGLU3", "FECHADO", "9.99"),
                ativo("ITUB4", "ABERTO", "0.10"),
                ativo("BOVA11", "ABERTO", "0.94"),
                ativo("HGLG11", "ABERTO", "0.50"));
        Map<String, Object> d = DashboardMontador.montar(Map.of(), List.of(), ativos);
        List<?> destaques = (List<?>) d.get("destaques");
        assertEquals(4, destaques.size());
        assertEquals("VALE3", ((Map<?, ?>) destaques.get(0)).get("ticker"));
        assertEquals("PETR4", ((Map<?, ?>) destaques.get(1)).get("ticker"));
        assertEquals("HGLG11", ((Map<?, ?>) destaques.get(3)).get("ticker"));
    }

    @Test
    void respostaFormatoErradoViraErro502() {
        BffException e = assertThrows(BffException.class,
                () -> DashboardMontador.montar(List.of(), List.of(), List.of()));
        assertEquals(502, e.getStatus());
    }

    @Test
    void entendeVariosFormatosDeData() {
        assertEquals(DashboardMontador.instante("2026-11-23T12:00:00Z"),
                DashboardMontador.instante("2026-11-23T09:00:00-03:00"));
        assertEquals(DashboardMontador.instante("2026-11-23T12:00:00Z"),
                DashboardMontador.instante("2026-11-23T12:00:00"));
    }
}
