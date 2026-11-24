package br.sentineltrade.bff.dashboard;

import br.sentineltrade.bff.comum.BffException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Monta a resposta de GET /dashboard juntando tres chamadas ao backend:
 * carteira + ordens + ativos  ->  {saldoDisponivel, saldoBloqueado, patrimonio, ordensRecentes[5], destaques[4]}
 *
 * Isto e AGREGACAO DE APRESENTACAO (papel do BFF), nao regra de negocio:
 * o BFF so soma e ordena valores que o backend ja calculou.
 */
public final class DashboardMontador {

    private static final int ORDENS_RECENTES = 5;
    private static final int DESTAQUES = 4;

    private DashboardMontador() {
    }

    public static Map<String, Object> montar(Object carteira, Object ordens, Object ativos) {
        Map<?, ?> c = comoMapa(carteira);
        BigDecimal disponivel = numero(c.get("saldoDisponivel"));
        BigDecimal bloqueado = numero(c.get("saldoBloqueado"));

        BigDecimal emAtivos = BigDecimal.ZERO;
        for (Object posicao : comoLista(c.get("posicoes"))) {
            emAtivos = emAtivos.add(numero(comoMapa(posicao).get("valorAtual")));
        }
        BigDecimal patrimonio = disponivel.add(bloqueado).add(emAtivos).setScale(2, RoundingMode.HALF_EVEN);

        // Ordens mais recentes primeiro
        Comparator<Object> porData = Comparator.comparing(o -> instante(comoMapa(o).get("criadoEm")));
        List<Object> recentes = new ArrayList<>(comoLista(ordens));
        recentes.sort(porData.reversed());
        recentes = new ArrayList<>(recentes.subList(0, Math.min(ORDENS_RECENTES, recentes.size())));

        // Destaques: ativos com mercado aberto que mais variaram (para cima ou para baixo)
        Comparator<Object> porVariacao = Comparator.comparing(a -> numero(comoMapa(a).get("variacao")).abs());
        List<Object> destaques = new ArrayList<>();
        for (Object ativo : comoLista(ativos)) {
            if ("ABERTO".equals(comoMapa(ativo).get("mercado"))) {
                destaques.add(ativo);
            }
        }
        destaques.sort(porVariacao.reversed());
        destaques = new ArrayList<>(destaques.subList(0, Math.min(DESTAQUES, destaques.size())));

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("saldoDisponivel", disponivel);
        resposta.put("saldoBloqueado", bloqueado);
        resposta.put("patrimonio", patrimonio);
        resposta.put("ordensRecentes", recentes);
        resposta.put("destaques", destaques);
        return resposta;
    }

    static Map<?, ?> comoMapa(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            return mapa;
        }
        throw respostaInesperada();
    }

    static List<?> comoLista(Object valor) {
        if (valor == null) {
            return List.of();
        }
        if (valor instanceof List<?> lista) {
            return lista;
        }
        throw respostaInesperada();
    }

    static BigDecimal numero(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        try {
            if (valor instanceof BigDecimal bd) {
                return bd;
            }
            if (valor instanceof Number || valor instanceof String) {
                return new BigDecimal(valor.toString());
            }
        } catch (NumberFormatException e) {
            throw respostaInesperada();
        }
        throw respostaInesperada();
    }

    static Instant instante(Object valor) {
        if (!(valor instanceof String texto)) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException e1) {
            try {
                return OffsetDateTime.parse(texto).toInstant();
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(texto).toInstant(ZoneOffset.UTC);
                } catch (DateTimeParseException e3) {
                    return Instant.EPOCH;
                }
            }
        }
    }

    private static BffException respostaInesperada() {
        return new BffException(502, "Resposta inesperada do servidor. Tente novamente.");
    }
}
