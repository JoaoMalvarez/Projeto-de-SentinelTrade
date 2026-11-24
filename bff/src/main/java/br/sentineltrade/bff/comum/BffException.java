package br.sentineltrade.bff.comum;

/**
 * Erro com status HTTP e mensagem segura para o usuario.
 * O ErroHandler transforma em { "message": "..." } com o status correto.
 */
public class BffException extends RuntimeException {

    private final int status;

    public BffException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
