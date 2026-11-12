package br.sentineltrade.mensageria;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome eventos da fila (envio a bolsa, notificacoes). RF06, RF07, RT01.
 * Processa mensagens de forma assíncrona utilizando Apache Kafka.
 */
@Component
public class ConsumidorEventos {

    /**
     * Ouve o tópico de ordens validadas para encaminhamento à bolsa simulada e notificações.
     */
    @KafkaListener(topics = "topico-ordens-validadas", groupId = "sentineltrade-group")
    public void consumirOrdemValidada(String mensagemEvento) {
        // Lógica de consumo do evento e integração assíncrona
        System.out.println("Evento consumido com sucesso: " + mensagemEvento);
    }
}