package br.sentineltrade.bolsa;

import org.springframework.stereotype.Service;
import java.util.Random;

/**
 * Bolsa/corretora simulada que recebe a ordem e devolve o resultado. RF06.
 * Simula o ambiente de execução e liquidação no livro de ofertas.
 */
@Service
public class BolsaSimulada {

    private final Random random = new Random();

    /**
     * Processa a ordem enviada à bolsa simulada.
     * Retorna o status simulado de execução (EXECUTADA ou REJEITADA).
     */
    public ResultadoSimulacao processarOrdem(OrdemBolsaDTO ordem) {
        // Simulação de latência de rede/mercado
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulação de motor de matching (90% de chance de sucesso, 10% de rejeição por liquidez)
        boolean sucesso = random.nextDouble() > 0.10;

        if (sucesso) {
            return new ResultadoSimulacao(ordem.idOrdem(), "EXECUTADA", "Ordem liquidada com sucesso no livro de ofertas.");
        } else {
            return new ResultadoSimulacao(ordem.idOrdem(), "REJEITADA", "Ordem rejeitada pela bolsa: falta de liquidez no preço limite.");
        }
    }
}

// DTOs auxiliares para comunicação com a bolsa simulada
record OrdemBolsaDTO(String idOrdem, String ticker, int quantidade, double precoLimite, String tipo) {}
record ResultadoSimulacao(String idOrdem, String status, String mensagem) {}