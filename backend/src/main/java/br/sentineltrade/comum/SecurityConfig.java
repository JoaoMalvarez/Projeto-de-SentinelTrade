package br.sentineltrade.comum;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração mínima de segurança: apenas /actuator/health é público.
 * Todo o resto exige autenticação.
 * 
 * TODO: adicionar filtro JWT, perfis (INVESTIDOR, AUDITOR, ADMIN) e MFA.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // API stateless; desabilitado para testes locais e JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
}
