package br.sentineltrade.bff.sessao;

import java.time.Instant;

/**
 * Sessao de um usuario no BFF.
 *
 * - id: token opaco e aleatorio que o navegador recebe (cookie httpOnly e/ou Bearer).
 * - tokenBackend: JWT emitido pelo backend. Fica SO no BFF, nunca vai para o navegador.
 */
public final class Sessao {

    private final String id;
    private final String tokenBackend;
    private final Usuario usuario;
    private final Instant criadaEm;
    private volatile Instant ultimoAcesso;

    Sessao(String id, String tokenBackend, Usuario usuario, Instant criadaEm) {
        this.id = id;
        this.tokenBackend = tokenBackend;
        this.usuario = usuario;
        this.criadaEm = criadaEm;
        this.ultimoAcesso = criadaEm;
    }

    public String id() {
        return id;
    }

    public String tokenBackend() {
        return tokenBackend;
    }

    public Usuario usuario() {
        return usuario;
    }

    public Instant criadaEm() {
        return criadaEm;
    }

    public Instant ultimoAcesso() {
        return ultimoAcesso;
    }

    void tocar(Instant agora) {
        this.ultimoAcesso = agora;
    }

    /** Nunca imprime tokens, para nao vazar em logs. */
    @Override
    public String toString() {
        return "Sessao{usuario=" + usuario.email() + ", perfil=" + usuario.perfil() + "}";
    }
}
