package br.sentineltrade.notificacao;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cria notificacoes para o investidor. RF07.
 * Gerencia o envio de alertas e avisos sobre eventos de ordens e execuções na plataforma.
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    /**
     * Cria e persiste uma nova notificação para o investidor.
     */
    public Notificacao criarNotificacao(UUID investidorId, String titulo, String mensagem) {
        Notificacao notificacao = new Notificacao(investidorId, titulo, mensagem, LocalDateTime.now(), false);
        return notificacaoRepository.save(notificacao);
    }
}

/**
 * Entidade de notificação do investidor.
 */
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "notificacao")
class Notificacao {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @jakarta.persistence.Column(name = "investidor_id", nullable = false)
    private UUID investidorId;

    @jakarta.persistence.Column(nullable = false, length = 150)
    private String titulo;

    @jakarta.persistence.Column(columnDefinition = "TEXT", nullable = false)
    private String mensagem;

    @jakarta.persistence.Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @jakarta.persistence.Column(nullable = false)
    private boolean lida;

    public Notificacao() {}

    public Notificacao(UUID investidorId, String titulo, String mensagem, LocalDateTime criadoEm, boolean lida) {
        this.investidorId = investidorId;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.criadoEm = criadoEm;
        this.lida = lida;
    }

    public UUID getId() { return id; }
    public UUID getInvestidorId() { return investidorId; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }
}

interface NotificacaoRepository extends org.springframework.data.jpa.repository.JpaRepository<Notificacao, UUID> {}