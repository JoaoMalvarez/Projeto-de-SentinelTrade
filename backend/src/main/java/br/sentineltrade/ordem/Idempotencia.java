package br.sentineltradeordem;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Prevencao de ordens duplicadas pela chave de idempotencia. RNF02.
 * Utiliza o Redis para garantir que a mesma ordem não seja processada mais de uma vez.
 */
@Component
public class Idempotencia {

    private final StringRedisTemplate redisTemplate;

    private static final String CHAVE_PREFIXO = "idempotencia:ordem:";
    private static final Duration TEMPO_EXPIRACAO = Duration.ofMinutes(10);

    public Idempotencia(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Verifica se a chave de idempotência já existe. Se não existir, registra e retorna true.
     * Se já existir, retorna false (indicando ordem duplicada).
     */
    public boolean tentarAdquirir(String chaveIdempotencia) {
        String chaveCompleta = CHAVE_PREFIXO + chaveIdempotencia;
        
        // setIfAbsent (SETNX) garante atomicidade no Redis
        Boolean sucesso = redisTemplate.opsForValue()
                .setIfAbsent(chaveCompleta, "PROCESSANDO", TEMPO_EXPIRACAO);
                
        return Boolean.TRUE.equals(sucesso);
    }

    /**
     * Libera a chave em caso de falha catastrófica para permitir nova tentativa.
     */
    public void liberar(String chaveIdempotencia) {
        String chaveCompleta = CHAVE_PREFIXO + chaveIdempotencia;
        redisTemplate.delete(chaveCompleta);
    }
}