package br.sentineltrade.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BFF (Backend For Frontend) do SentinelTrade.
 *
 * Fica entre o frontend (Vue) e o backend (Spring Boot):
 *   Vue --HTTPS--> BFF --HTTP interno--> Backend
 *
 * Regra de ouro: o BFF NAO decide nada de negocio. Ele traduz, agrega e protege.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class BffApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
