package br.sentineltrade.bff.comum;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validacao de parametros de URL, query string e cabecalhos.
 * (O corpo das requisicoes e validado com @Valid nos DTOs.)
 *
 * Tudo que nao passa aqui e recusado com 400 antes de chegar ao backend.
 */
public final class Validacoes {

    private static final Pattern TICKER = Pattern.compile("^[A-Z0-9]{4,12}$");
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");
    private static final Pattern CHAVE_IDEMPOTENCIA = Pattern.compile("^[A-Za-z0-9-]{8,100}$");
    private static final Pattern EVENTO = Pattern.compile("^[A-Z_]{1,60}$");
    private static final Pattern CONTROLE = Pattern.compile("\\p{Cntrl}");

    public static final Set<String> ABAS = Set.of("todas", "andamento", "historico");
    public static final Set<String> STATUS_ORDEM = Set.of("PENDENTE", "VALIDADA", "ENVIADA_BOLSA",
            "PARCIALMENTE_EXECUTADA", "EXECUTADA", "REJEITADA", "CANCELADA");
    public static final Set<String> TIPOS_ORDEM = Set.of("COMPRA", "VENDA");

    private Validacoes() {
    }

    /** Ticker como PETR4, BOVA11. Aceita minusculas e devolve em maiusculas. */
    public static String ticker(String valor) {
        String t = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!TICKER.matcher(t).matches()) {
            throw new BffException(400, "Ticker inválido.");
        }
        return t;
    }

    /** Identificador (UUID ou codigo alfanumerico com hifen). */
    public static String id(String valor) {
        if (valor == null || !ID.matcher(valor).matches()) {
            throw new BffException(400, "Identificador inválido.");
        }
        return valor;
    }

    /** Chave de idempotencia enviada pelo frontend (um UUID por tentativa de ordem). */
    public static String chaveIdempotencia(String valor) {
        if (valor == null || !CHAVE_IDEMPOTENCIA.matcher(valor).matches()) {
            throw new BffException(400, "Cabeçalho Idempotency-Key ausente ou inválido.");
        }
        return valor;
    }

    /** Filtro opcional que so pode assumir valores de uma lista fechada. Vazio = sem filtro. */
    public static String opcional(String valor, Set<String> permitidos, String nome) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        if (!permitidos.contains(valor)) {
            throw new BffException(400, "Filtro inválido: " + nome + ".");
        }
        return valor;
    }

    /** Nome de evento de auditoria (ex.: LOGIN_OK). Vazio = sem filtro. */
    public static String eventoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        if (!EVENTO.matcher(valor).matches()) {
            throw new BffException(400, "Filtro inválido: evento.");
        }
        return valor;
    }

    /** Texto livre de busca: tira caracteres de controle e limita o tamanho. Vazio = sem filtro. */
    public static String textoBusca(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        String limpo = semControle(valor).trim();
        if (limpo.isEmpty()) {
            return null;
        }
        if (limpo.length() > maximo) {
            throw new BffException(400, "Texto de busca muito longo.");
        }
        return limpo;
    }

    /** Remove caracteres de controle (quebras de linha etc.), evitando injecao em logs e cabecalhos. */
    public static String semControle(String valor) {
        return valor == null ? null : CONTROLE.matcher(valor).replaceAll("");
    }

    public static String truncar(String valor, int maximo) {
        if (valor == null || valor.length() <= maximo) {
            return valor;
        }
        return valor.substring(0, maximo);
    }
}
