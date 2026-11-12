package br.sentineltrade.conta;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade Investidor (tabela investidor). RF02.
 * Representa o investidor cadastrado na plataforma SentinelTrade.
 */
@Entity
@Table(name = "investidor")
public class Investidor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "mfa_secret", nullable = false)
    private String mfaSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilInvestidor perfil;

    public Investidor() {
    }

    public Investidor(UUID id, String nomeCompleto, String email, String senhaHash, String mfaSecret, PerfilInvestidor perfil) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.senhaHash = senhaHash;
        this.mfaSecret = mfaSecret;
        this.perfil = perfil;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public PerfilInvestidor getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilInvestidor perfil) {
        this.perfil = perfil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Investidor that = (Investidor) o;
        return Objects.equals(id, that.id) || Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "Investidor{" +
                "id=" + id +
                ", nomeCompleto='" + nomeCompleto + '\'' +
                ", email='" + email + '\'' +
                ", perfil=" + perfil +
                '}';
    }
}

enum PerfilInvestidor {
    INVESTIDOR,
    AUDITOR,
    ADMIN
}