package br.sentineltrade.conta;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade Conta com saldo disponivel e bloqueado (tabela conta). RF02.
 * Representa a conta corrente e os saldos financeiros do investidor.
 */
@Entity
@Table(name = "conta")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "investidor_id", nullable = false)
    private UUID investidorId;

    @Column(name = "saldo_disponivel", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoDisponivel;

    @Column(name = "saldo_bloqueado", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoBloqueado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConta status;

    public Conta() {
    }

    public Conta(UUID id, UUID investidorId, BigDecimal saldoDisponivel, BigDecimal saldoBloqueado, StatusConta status) {
        this.id = id;
        this.investidorId = investidorId;
        this.saldoDisponivel = saldoDisponivel;
        this.saldoBloqueado = saldoBloqueado;
        this.status = status;
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

    public BigDecimal getSaldoDisponivel() {
        return saldoDisponivel;
    }

    public void setSaldoDisponivel(BigDecimal saldoDisponivel) {
        this.saldoDisponivel = saldoDisponivel;
    }

    public BigDecimal getSaldoBloqueado() {
        return saldoBloqueado;
    }

    public void setSaldoBloqueado(BigDecimal saldoBloqueado) {
        this.saldoBloqueado = saldoBloqueado;
    }

    public StatusConta getStatus() {
        return status;
    }

    public void setStatus(StatusConta status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conta conta = (Conta) o;
        return Objects.equals(id, conta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Conta{" +
                "id=" + id +
                ", investidorId=" + investidorId +
                ", saldoDisponivel=" + saldoDisponivel +
                ", saldoBloqueado=" + saldoBloqueado +
                ", status=" + status +
                '}';
    }
}

enum StatusConta {
    ATIVA,
    BLOQUEADA,
    ENCERRADA
}