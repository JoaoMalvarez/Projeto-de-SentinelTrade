package br.sentineltrade.conta;

import br.sentineltrade.ativo.Ativo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de posicao em carteira (tabela carteira_posicao). RF02.
 * Representa os ativos custodiados pelo investidor na plataforma.
 */
@Entity
@Table(name = "carteira_posicao")
public class CarteiraPosicao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "investidor_id", nullable = false)
    private UUID investidorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_id", nullable = false)
    private Ativo ativo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_medio", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoMedio;

    public CarteiraPosicao() {
    }

    public CarteiraPosicao(UUID id, UUID investidorId, Ativo ativo, Integer quantidade, BigDecimal precoMedio) {
        this.id = id;
        this.investidorId = investidorId;
        this.ativo = ativo;
        this.quantidade = quantidade;
        this.precoMedio = precoMedio;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInvestidorId() {
        return investidorId;
    }

    public void setInvestidorId(UUID investidorId) {
        this.investidorId = investidorId;
    }

    public Ativo getAtivo() {
        return ativo;
    }

    public void setAtivo(Ativo ativo) {
        this.ativo = ativo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoMedio() {
        return precoMedio;
    }

    public void setPrecoMedio(BigDecimal precoMedio) {
        this.precoMedio = precoMedio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarteiraPosicao that = (CarteiraPosicao) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CarteiraPosicao{" +
                "id=" + id +
                ", investidorId=" + investidorId +
                ", ativo=" + (ativo != null ? ativo.getTicker() : null) +
                ", quantidade=" + quantidade +
                ", precoMedio=" + precoMedio +
                '}';
    }
}