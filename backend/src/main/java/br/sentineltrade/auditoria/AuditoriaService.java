package br.sentineltrade.auditoria;

import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço e Entidade de Auditoria imutável (tabela auditoria_log). RF08, RN02.
 * Garante a imutabilidade estilo WORM (Write Once, Read Many), permitindo apenas inserções e leituras.
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    /**
     * Registra um evento de auditoria de forma isolada e imutável.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditoriaLog registrarEvento(UUID usuarioId, String acao, String detalhes) {
        AuditoriaLog log = new AuditoriaLog(usuarioId, acao, detalhes, LocalDateTime.now());
        return auditoriaRepository.save(log);
    }
}

/**
 * Entidade que mapeia a tabela de auditoria WORM (Write Once, Read Many).
 */
@Entity
@Table(name = "auditoria_log")
class AuditoriaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 100)
    private String acao;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String detalhes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public AuditoriaLog() {
    }

    public AuditoriaLog(UUID usuarioId, String acao, String detalhes, LocalDateTime criadoEm) {
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.detalhes = detalhes;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getAcao() {
        return acao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditoriaLog that = (AuditoriaLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditoriaLog{" +
                "id=" + id +
                ", usuarioId=" + usuarioId +
                ", acao='" + acao + '\'' +
                ", criadoEm=" + criadoEm +
                '}';
    }
}

interface AuditoriaRepository extends org.springframework.data.jpa.repository.JpaRepository<AuditoriaLog, UUID> {
    // Restrito apenas a inserção e leitura (WORM) - sem métodos de Update ou Delete
}