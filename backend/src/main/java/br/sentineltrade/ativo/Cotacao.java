package br.sentineltrade.ativo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Cotacao recebida do provedor simulado (tabela cotacao_historico).
 * Representa os registros históricos ou em tempo quase real de preços dos ativos.
 */
@Entity
@Table(name = "cotacao_historico")
public class Cotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_id", nullable = false)
    private Ativo ativo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal volume;

    @Column(name = "recebido_em", nullable = false)
    private LocalDateTime recebidoEm = LocalDateTime.now();

    public Cotacao() {
    }

    public Cotacao(UUID id, Ativo ativo, BigDecimal preco, BigDecimal volume, LocalDateTime recebidoEm) {
        this.id = id;
        this.ativo = ativo;
        this.preco = preco;
        this.volume = volume;
        this.recebidoEm = recebidoEm;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Ativo getAtivo() {
        return ativo;
    }

    public void setAtivo(Ativo ativo) {
        this.ativo = ativo;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public LocalDateTime getRecebidoEm() {
        return recebidoEm;
    }

    public void setRecebidoEm(LocalDateTime recebidoEm) {
        this.recebidoEm = recebidoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cotacao cotacao = (Cotacao) o;
        return Objects.equals(id, cotacao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Cotacao{" +
                "id=" + id +
                ", ativo=" + (ativo != null ? ativo.getTicker() : null) +
                ", preco=" + preco +
                ", volume=" + volume +
                ", recebidoEm=" + recebidoEm +
                '}';
    }
}