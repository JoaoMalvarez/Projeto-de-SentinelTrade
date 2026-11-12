package br.sentineltrade.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AuthRepository extends JpaRepository<CredencialMfa, UUID> {
    Optional<CredencialMfa> findByEmail(String email);
}   