package br.sentineltrade.comum;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * Utilitários compartilhados (ex.: sanitização de entrada).
 */
@Component
public class Utils {

    /**
     * Sanitiza entradas de texto removendo caracteres especiais indesejados
     * e prevenindo potenciais ataques de injeção ou XSS nas requisições.
     */
    public String sanitizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        
        // Remove acentos e caracteres diacríticos
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        String semAcentos = normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // Remove caracteres perigosos ou tags HTML básicas
        return semAcentos.replaceAll("[<>\"'%;()&+]", "").trim();
    }
}