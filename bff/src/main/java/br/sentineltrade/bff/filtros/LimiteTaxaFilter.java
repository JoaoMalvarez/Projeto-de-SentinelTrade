package br.sentineltrade.bff.filtros;

import br.sentineltrade.bff.comum.RedeUtil;
import br.sentineltrade.bff.config.BffProperties;
import br.sentineltrade.bff.seguranca.LimitadorTaxa;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Primeira barreira contra abuso:
 *  - rate limit por IP (mais rigoroso em /api/auth/*, contra forca bruta de senha);
 *  - tamanho maximo do corpo da requisicao.
 */
public class LimiteTaxaFilter extends OncePerRequestFilter {

    private final LimitadorTaxa limitador;
    private final BffProperties props;

    public LimiteTaxaFilter(LimitadorTaxa limitador, BffProperties props) {
        this.limitador = limitador;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain cadeia)
            throws ServletException, IOException {
        if ("OPTIONS".equals(req.getMethod())) { // pre-flight do CORS
            cadeia.doFilter(req, res);
            return;
        }

        long tamanho = req.getContentLengthLong();
        if (tamanho > props.limite().corpoMaxBytes()) {
            responder(res, 413, "Requisição grande demais.");
            return;
        }
        // Corpo sem tamanho declarado (chunked) nao tem como ser limitado antes de ler: recusa.
        if (tamanho < 0 && req.getHeader("Transfer-Encoding") != null) {
            responder(res, 411, "Informe o tamanho da requisição.");
            return;
        }

        boolean auth = req.getRequestURI().startsWith("/api/auth/");
        int limite = auth ? props.limite().authPorMinuto() : props.limite().geralPorMinuto();
        String chave = RedeUtil.ipCliente(req, props.confiarProxy()) + (auth ? "|auth" : "|geral");

        LimitadorTaxa.Resultado r = limitador.tentar(chave, limite);
        if (!r.permitido()) {
            res.setHeader("Retry-After", String.valueOf(r.aguardarSegundos()));
            responder(res, 429, "Muitas tentativas. Aguarde um instante e tente novamente.");
            return;
        }
        cadeia.doFilter(req, res);
    }

    private static void responder(HttpServletResponse res, int status, String mensagem) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Mensagens fixas e sem aspas: e seguro montar o JSON a mao.
        res.getWriter().write("{\"message\":\"" + mensagem + "\"}");
    }
}
