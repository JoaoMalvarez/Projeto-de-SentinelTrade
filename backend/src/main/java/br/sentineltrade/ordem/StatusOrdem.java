package br.sentineltrade.ordem;

/**
 * Estados do ciclo de vida da ordem e transicoes permitidas. RF04.
 * Define os possíveis status e regras de transição de uma ordem no sistema.
 */
public enum StatusOrdem {
    PENDENTE("Pendente de validação"),
    VALIDADA("Validada pelo motor de risco"),
    ENVIADA_BOLSA("Enviada para a bolsa simulada"),
    EXECUTADA("Executada com sucesso"),
    REJEITADA("Rejeitada pelo risco ou pela bolsa"),
    CANCELADA("Cancelada pelo investidor");

    private final String descricao;

    StatusOrdem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Valida se a transição de status é permitida no ciclo de vida da ordem.
     */
    public boolean podeTransicionarPara(StatusOrdem novoStatus) {
        return switch (this) {
            case PENDENTE -> novoStatus == VALIDADA || novoStatus == REJEITADA || novoStatus == CANCELADA;
            case VALIDADA -> novoStatus == ENVIADA_BOLSA || novoStatus == CANCELADA;
            case ENVIADA_BOLSA -> novoStatus == EXECUTADA || novoStatus == REJEITADA;
            case EXECUTADA, REJEITADA, CANCELADA -> false; // Estados finais
        };
    }
}