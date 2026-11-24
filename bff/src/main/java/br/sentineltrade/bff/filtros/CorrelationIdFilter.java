package br.sentineltrade.bff.filtros;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Da a cada requisicao um X-Correlation-Id (aproveita o do cliente se for valido),
 * coloca no log (MDC), devolve no cabecalho da resposta e repassa ao backend.
 * Assim da para seguir uma operacao do navegador ate o banco pelos logs.
 *
 * Tambem escreve o log de acesso: metodo, caminho, status e tempo.
 * Nunca loga corpo, senha, token ou codigo MFA.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Correlation-Id";
    public static final String MDC_CHAVE = "correlationId";

    private static final Pattern VALIDO = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final Logger log = LoggerFactory.getLogger("acesso");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain cadeia)
            throws ServletException, IOException {
        String recebido = req.getHeader(CABECALHO);
        String id = recebido != null && VALIDO.matcher(recebido).matches() ? recebido : UUID.randomUUID().toString();

        MDC.put(MDC_CHAVE, id);
        res.setHeader(CABECALHO, id);
        long inicio = System.nanoTime();
        try {
            cadeia.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            log.info("{} {} -> {} ({} ms)", req.getMethod(), req.getRequestURI(), res.getStatus(), ms);
            MDC.remove(MDC_CHAVE);
        }
    }
}
