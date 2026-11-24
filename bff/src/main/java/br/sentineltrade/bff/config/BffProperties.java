package br.sentineltrade.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuracoes do BFF, lidas do application.yml (prefixo "bff").
 * Os valores sensiveis vem do .env (variaveis de ambiente), nunca do codigo.
 */
@ConfigurationProperties(prefix = "bff")
public record BffProperties(
        String backendUrl,
        String chaveInterna,
        List<String> origensFrontend,
        boolean cookieSeguro,
        boolean confiarProxy,
        Duration timeoutConexao,
        Duration timeoutLeitura,
        SessaoProps sessao,
        LimiteProps limite,
        CircuitoProps circuito,
        CotacoesProps cotacoes) {

    public record SessaoProps(Duration inatividade, Duration duracaoMaxima) {}

    public record LimiteProps(int authPorMinuto, int geralPorMinuto, long corpoMaxBytes) {}

    public record CircuitoProps(int falhasParaAbrir, Duration tempoAberto) {}

    public record CotacoesProps(long intervaloMs, int maxConexoes, Duration timeoutConexao) {}
}
