package br.sentineltrade.bff.sessao;

import java.util.Arrays;
import java.util.Set;

/** Dados publicos do usuario logado, como o frontend espera: {id, nome, email, perfil}. */
public record Usuario(String id, String nome, String email, String perfil) {

    public static final Set<String> PERFIS = Set.of("INVESTIDOR", "AUDITOR", "ADMIN");

    public boolean temPerfil(String... perfis) {
        return Arrays.asList(perfis).contains(perfil);
    }
}
