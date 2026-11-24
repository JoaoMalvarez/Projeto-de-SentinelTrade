package br.sentineltrade.bff.config;

import br.sentineltrade.bff.backend.CircuitBreaker;
import br.sentineltrade.bff.filtros.CabecalhosSegurancaFilter;
import br.sentineltrade.bff.filtros.CorrelationIdFilter;
import br.sentineltrade.bff.filtros.LimiteTaxaFilter;
import br.sentineltrade.bff.seguranca.LimitadorTaxa;
import br.sentineltrade.bff.sessao.SessaoStore;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Monta as pecas do BFF: cliente HTTP do backend, sessoes, limitador e filtros.
 *
 * Ordem dos filtros (cada requisicao passa por eles nesta ordem):
 *   1. CorrelationId  - gera/propaga X-Correlation-Id e escreve o log de acesso
 *   2. CORS           - so o frontend pode chamar o BFF pelo navegador
 *   3. Cabecalhos     - CSP, HSTS, X-Content-Type-Options...
 *   4. Limite de taxa - rate limit por IP e tamanho maximo do corpo
 */
@Configuration
public class BffConfig {

    @Bean
    public SessaoStore sessaoStore(BffProperties props) {
        return new SessaoStore(props.sessao().inatividade(), props.sessao().duracaoMaxima(), Clock.systemUTC());
    }

    @Bean
    public LimitadorTaxa limitadorTaxa() {
        return new LimitadorTaxa(Duration.ofMinutes(1), System::currentTimeMillis);
    }

    @Bean
    public CircuitBreaker circuitBreaker(BffProperties props) {
        return new CircuitBreaker(props.circuito().falhasParaAbrir(), props.circuito().tempoAberto(),
                System::currentTimeMillis);
    }

    /** Cliente HTTP para o backend, com timeout de conexao e de leitura. */
    @Bean
    public RestClient restClient(BffProperties props) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(props.timeoutConexao())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(props.timeoutLeitura());
        return RestClient.builder().requestFactory(fabrica).build();
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> filtroCorrelationId() {
        FilterRegistrationBean<CorrelationIdFilter> registro = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registro.setOrder(1);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> filtroCors(BffProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(props.origensFrontend());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "Authorization", "Idempotency-Key", "X-Correlation-Id"));
        cors.setExposedHeaders(List.of("X-Correlation-Id", "Retry-After"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", cors);

        FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(new CorsFilter(fonte));
        registro.setOrder(2);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<CabecalhosSegurancaFilter> filtroCabecalhos() {
        FilterRegistrationBean<CabecalhosSegurancaFilter> registro =
                new FilterRegistrationBean<>(new CabecalhosSegurancaFilter());
        registro.setOrder(3);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<LimiteTaxaFilter> filtroLimiteTaxa(LimitadorTaxa limitador, BffProperties props) {
        FilterRegistrationBean<LimiteTaxaFilter> registro =
                new FilterRegistrationBean<>(new LimiteTaxaFilter(limitador, props));
        registro.setOrder(4);
        return registro;
    }
}
