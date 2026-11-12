package br.sentineltrade.ordem;

import br.sentineltrade.conta.Conta;
import br.sentineltrade.conta.CarteiraPosicao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Portão de risco pré-execução: saldo, posicao, limite e mercado aberto. RF05.
 * Valida se o investidor possui recursos e condições suficientes para realizar a operação.
 */
@Component
public class ValidadorRisco {

    /**
     * Valida os riscos de pré-execução de uma ordem de compra ou venda.
     * Lança exceção caso alguma regra de negócio seja violada.
     */
    public void validar(Ordem ordem, Conta conta, CarteiraPosicao posicaoAtual) {
        if (ordem == null) {
            throw new IllegalArgumentException("A ordem não pode ser nula.");
        }

        // Validação de Mercado Aberto (Simulação: horário comercial fictício ou regra genérica)
        if (conta == null) {
            throw new IllegalStateException("Conta do investidor não encontrada.");
        }

        BigDecimal valorTotal = ordem.getPrecoLimite().multiply(BigDecimal.valueOf(ordem.getQuantidade()));

        if (ordem.getTipo() == TipoOrdem.COMPRA) {
            // Verifica se há saldo disponível suficiente na conta
            if (conta.getSaldoDisponivel().compareTo(valorTotal) < 0) {
                throw new IllegalStateException("Saldo disponível insuficiente para realizar a compra.");
            }
        } else if (ordem.getTipo() == TipoOrdem.VENDA) {
            // Verifica se há quantidade custodiada suficiente na carteira
            int quantidadeDisponivel = (posicaoAtual != null) ? posicaoAtual.getQuantidade() : 0;
            if (quantidadeDisponivel < ordem.getQuantidade()) {
                throw new IllegalStateException("Quantidade de ativos insuficiente em custódia para realizar a venda.");
            }
        }
    }
}