package br.sentineltrade.auth;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entidade de controle de acesso, senhas com hash e segredo MFA. RF01.
 */
@Entity
@Table(name = "investidor_auth")
public class CredencialMfa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "mfa_secret", nullable = false)
    private String mfaSecret;

    public CredencialMfa() {}

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public String getMfaSecret() { return mfaSecret; }
}