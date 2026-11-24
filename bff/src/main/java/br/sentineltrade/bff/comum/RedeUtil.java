package br.sentineltrade.bff.comum;

import jakarta.servlet.http.HttpServletRequest;

/** Utilitarios de rede. */
public final class RedeUtil {

    private RedeUtil() {
    }

    /**
     * IP do cliente. So confia no X-Forwarded-For se o BFF estiver atras de um proxy
     * reverso confiavel (bff.confiar-proxy=true). Caso contrario, qualquer um poderia
     * forjar o cabecalho e burlar o rate limit.
     */
    public static String ipCliente(HttpServletRequest req, boolean confiarProxy) {
        if (confiarProxy) {
            String encaminhado = req.getHeader("X-Forwarded-For");
            if (encaminhado != null && !encaminhado.isBlank()) {
                String primeiro = encaminhado.split(",")[0].trim();
                if (!primeiro.isEmpty()) {
                    return Validacoes.truncar(Validacoes.semControle(primeiro), 64);
                }
            }
        }
        return req.getRemoteAddr();
    }
}
