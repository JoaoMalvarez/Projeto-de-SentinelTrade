package br.sentineltrade.bff.config;

import br.sentineltrade.bff.seguranca.LimitadorTaxa;
import br.sentineltrade.bff.sessao.SessaoStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Limpeza periodica: sessoes expiradas e contadores antigos do rate limit. */
@Component
public class Manutencao {

    private final SessaoStore sessoes;
    private final LimitadorTaxa limitador;

    public Manutencao(SessaoStore sessoes, LimitadorTaxa limitador) {
        this.sessoes = sessoes;
        this.limitador = limitador;
    }

    @Scheduled(fixedRate = 60_000)
    public void limpar() {
        sessoes.limparExpiradas();
        limitador.limparAntigas();
    }
}
