package br.sentineltrade.ordem;

import br.sentineltrade.ativo.Ativo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade Ordem (tabela ordem). RF04.
 * Representa as ordens de compra e venda enviadas pelos investidores.
 */
@Entity
@Table(name = "ordem")
public class Ordem {

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

    @Column(name = "preco_limite", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOrdem tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrdem status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Ordem() {
    }

    public Ordem(UUID investidorId, Ativo ativo, Integer quantidade, BigDecimal precoLimite, TipoOrdem tipo, StatusOrdem status) {
        this.investidorId = investidorId;
        this.ativo = ativo;
        this.quantidade = quantidade;
        this.precoLimite = precoLimite;
        this.tipo = tipo;
        this.status = status;
        this.criadoEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInvestidorId() {
        return investidorId;
    }

    public Ativo getAtivo() {
        return ativo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoLimite() {
        return precoLimite;
    }

    public TipoOrdem getTipo() {
        return tipo;
    }

    public StatusOrdem getStatus() {
        return status;
    }

    public void setStatus(StatusOrdem status) {
        this.status = status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ordem ordem = (Ordem) o;
        return Objects.equals(id, ordem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ordem{" +
                "id=" + id +
                ", investidorId=" + investidorId +
                ", ativo=" + (ativo != null ? ativo.getTicker() : null) +
                ", quantidade=" + quantidade +
                ", precoLimite=" + precoLimite +
                ", tipo=" + tipo +
                ", status=" + status +
                '}';
    }
}

enum TipoOrdem {
    COMPRA,
    VENDA
}