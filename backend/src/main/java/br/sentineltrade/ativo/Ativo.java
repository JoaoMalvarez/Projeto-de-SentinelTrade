package br.sentineltrade.ativo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade Ativo (tabela ativo). RF03.
 * Representa os ativos financeiros (Ações, ETFs, FIIs) negociados na plataforma SentinelTrade.
 */
@Entity
@Table(name = "ativos")
public class Ativo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAtivo tipo;

    @Column(name = "preco_atual", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoAtual;

    public Ativo() {
    }

    public Ativo(UUID id, String ticker, String nome, TipoAtivo tipo, BigDecimal precoAtual) {
        this.id = id;
        this.ticker = ticker;
        this.nome = nome;
        this.tipo = tipo;
        this.precoAtual = precoAtual;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoAtivo getTipo() {
        return tipo;
    }

    public void setTipo(TipoAtivo tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getPrecoAtual() {
        return precoAtual;
    }

    public void setPrecoAtual(BigDecimal precoAtual) {
        this.precoAtual = precoAtual;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ativo ativo = (Ativo) o;
        return Objects.equals(id, ativo.id) || Objects.equals(ticker, ativo.ticker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ticker);
    }

    @Override
    public String toString() {
        return "Ativo{" +
                "id=" + id +
                ", ticker='" + ticker + '\'' +
                ", nome='" + nome + '\'' +
                ", tipo=" + tipo +
                ", precoAtual=" + precoAtual +
                '}';
    }
}