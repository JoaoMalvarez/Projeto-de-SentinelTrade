# O que o Backend e o BFF precisam fazer

Este documento descreve tudo o que o frontend (já pronto, em modo simulado) espera do **BFF** e do **backend**.
O contrato abaixo é exatamente o que `frontend/src/services/api.js` chama. Para ligar, basta `VITE_USE_MOCK=false`.

```
Vue (frontend) ──HTTPS──▶ BFF ──HTTP interno──▶ Backend (Spring Boot)
                                                   ├─ PostgreSQL
                                                   ├─ Fila (RabbitMQ/Kafka)
                                                   ├─ Provedor de cotações (simulado)
                                                   └─ Bolsa simulada
```

## 1. Quem faz o quê

| Responsabilidade | BFF | Backend |
|---|---|---|
| Regras de negócio (saldo, posição, risco, estados da ordem) | ❌ nunca | ✅ |
| Persistência, transações, banco | ❌ | ✅ |
| Senha, MFA, emissão e validação do token | repassa e guarda a sessão | ✅ valida e emite |
| Formato das respostas para as telas | ✅ monta e agrega | fornece dados de domínio |
| Agregar várias chamadas em uma (dashboard) | ✅ | |
| Cotações em tempo real para o navegador | ✅ expõe SSE/WebSocket | ✅ fonte dos preços |
| Idempotência | repassa o `Idempotency-Key` | ✅ garante unicidade |
| Auditoria | envia contexto (IP, user-agent, correlation-id) | ✅ grava |
| CORS, rate limit por IP, sanitização de entrada | ✅ primeira barreira | ✅ revalida tudo |

Regra de ouro: **o BFF não decide nada de negócio**. Ele traduz e protege. O backend nunca confia no BFF nem no frontend.

## 2. Contrato exposto pelo BFF (`/api/...`)

Erros sempre no formato `{ "message": "texto para o usuário" }` com o status HTTP correto.

### Autenticação (RF01)
| Método e rota | Corpo | Resposta | Observação |
|---|---|---|---|
| `POST /auth/login` | `{email, senha}` | `{mfaToken}` | Mensagem genérica "Credenciais inválidas." (não revelar se o e-mail existe). Bloqueio temporário após N falhas |
| `POST /auth/mfa` | `{mfaToken, codigo}` | `{token, usuario:{id,nome,email,perfil}}` | `codigo` TOTP de 6 dígitos. `mfaToken` curto (poucos minutos) e de uso único |
| `POST /auth/cadastro` | `{nome, email, senha}` | `201 {cadastroToken, mfa:{segredo, otpauthUri}}` | Valida (nome ≥ 3 letras, e-mail, senha forte), grava a conta como **pendente** e gera o segredo TOTP. `409` se o e-mail já existir |
| `POST /auth/cadastro/mfa` | `{cadastroToken, codigo}` | `201 {mensagem, saldoInicial}` | Só aqui a conta é **ativada**, após o usuário provar que configurou o autenticador (código TOTP válido) |
| `POST /auth/esqueci-senha` | `{email}` | `202 {mensagem}` | Sempre a mesma resposta, exista ou não o e-mail |
| `POST /auth/logout` | — | `204` | Invalida o token |

### Investidor
| Rota | Resposta | Perfil |
|---|---|---|
| `GET /dashboard` | `{saldoDisponivel, saldoBloqueado, patrimonio, ordensRecentes[5], destaques[4]}` | INVESTIDOR |
| `GET /carteira` | `{saldoDisponivel, saldoBloqueado, limites:{porOrdem,diario,ordensDia}, posicoes:[{ticker,nome,quantidade,precoMedio,precoAtual,valorAtual,resultado,resultadoPct}]}` | INVESTIDOR |
| `GET /ativos` | `[{ticker,nome,tipo,preco,variacao,mercado,atualizadoEm}]` | INVESTIDOR |
| `GET /ativos/:ticker` | ativo + `serie[30]` (preços) + `posicao:{quantidade,precoMedio}\|null` | INVESTIDOR |
| `GET /cotacoes/stream` | SSE: cada evento é a lista de ativos atualizada | INVESTIDOR |
| `POST /ordens` + header `Idempotency-Key` | `{ticker,tipo,quantidade,precoLimite}` | `201` com a ordem (pode vir `REJEITADA` + `motivoRejeicao`) | INVESTIDOR |
| `GET /ordens?aba&status&tipo` | lista de ordens (`aba`: todas, andamento, historico) | INVESTIDOR |
| `GET /ordens/:id` | ordem completa com `eventos[{status,em,detalhe}]` | INVESTIDOR (só as suas) |
| `DELETE /ordens/:id` | ordem `CANCELADA`; `409` se não puder mais cancelar | INVESTIDOR |
| `GET /notificacoes` | `[{id,ordemId,mensagem,lida,criadaEm}]` | INVESTIDOR |
| `PATCH /notificacoes/:id/lida` | `204` | INVESTIDOR |

Formato da **ordem**: `{id, ticker, tipo, quantidade, quantidadeExecutada, precoLimite, precoExecucao, status, motivoRejeicao, criadoEm, eventos[]}`.
Status: `PENDENTE, VALIDADA, ENVIADA_BOLSA, PARCIALMENTE_EXECUTADA, EXECUTADA, REJEITADA, CANCELADA`.

### Auditor e Admin
| Rota | Perfil |
|---|---|
| `GET /auditoria?evento&q` → `[{id,ocorridoEm,usuario,evento,entidade,entidadeId,hashAnterior,hashRegistro}]` | AUDITOR, ADMIN |
| `GET /auditoria/integridade` → `{ok, verificados, falhaEm?}` | AUDITOR, ADMIN |
| `GET /admin/estado` → `{modoSeguro, ativos[], investidores:[{id,nome,email,limites}]}` | ADMIN |
| `PUT /admin/investidores/:id/limites` `{porOrdem,diario,ordensDia}` | ADMIN |
| `PUT /admin/ativos/:ticker/mercado` `{status:'ABERTO'\|'FECHADO'}` | ADMIN |
| `PUT /admin/modo-seguro` `{ativo:boolean}` | ADMIN |

### Códigos HTTP que o frontend trata
`400` dados inválidos · `401` sessão expirada (vai para o login) · `403` sem permissão (tela 403) · `404` não encontrado · `409` conflito (ex.: cancelar ordem final) · `422` regra violada · `503` modo seguro/indisponível (tela de indisponibilidade).

## 3. Tudo o que o BFF precisa fazer

1. **Expor o contrato da seção 2** e traduzir cada rota para chamadas ao backend.
2. **Sessão e token:** preferencialmente guardar o JWT em **cookie `httpOnly; Secure; SameSite=Strict`** (o frontend já usa `credentials: 'include'`). Sem cookie, aceitar `Authorization: Bearer`. Renovar/expirar o token.
3. **Agregar:** `GET /dashboard` junta saldo, ordens recentes e destaques em uma resposta só.
4. **Cotações em tempo quase real:** manter uma conexão com o backend/provedor e fazer *fan-out* por SSE (`/cotacoes/stream`) para os navegadores. Reenviar a lista a cada atualização.
5. **Repassar `Idempotency-Key`** do cabeçalho até o backend, sem alterar.
6. **Autorização por perfil** como primeira barreira (INVESTIDOR, AUDITOR, ADMIN). O backend revalida.
7. **Validar e sanitizar entradas** (tipos, tamanhos, formato de ticker, números positivos) antes de repassar.
8. **Proteções:** CORS restrito à origem do frontend, rate limit (principalmente `/auth/*`), limite de tamanho de corpo, cabeçalhos de segurança (CSP, HSTS, X-Content-Type-Options), timeouts e *circuit breaker* para o backend.
9. **Normalizar erros** para `{message}` e mapear exceções do backend para os códigos HTTP acima. Nunca vazar stack trace.
10. **Correlation-id** por requisição (gerar `X-Correlation-Id` e propagar), mais IP e user-agent repassados ao backend para a auditoria.
11. **Modo seguro:** se o backend responder `503`, devolver `503` com mensagem amigável.
12. **Logs** sem dados sensíveis (nada de senha, token, código MFA).
13. **Tecnologia:** como o enunciado pede Java ou Python, o mais simples é outro projeto **Spring Boot** (Spring Web + `WebClient`, SSE com `SseEmitter`) na pasta `bff/`.

## 4. Tudo o que o Backend precisa fazer (por módulo)

### Autenticação (`auth`) · RF01
- Cadastro/consulta do investidor; **senha com hash BCrypt** (a coluna é `senha_hash`).
- **Login em duas etapas:** valida senha, emite `mfaToken` curto; valida **TOTP** (segredo em `mfa_secret`, criptografado em repouso); só então emite o **JWT** com perfil e expiração.
- **Bloqueio por tentativas** (ex.: 5 falhas → bloqueio de alguns minutos). Mensagens genéricas.
- **Cadastro em duas etapas:** (1) validar entrada e **política de senha** (mín. 8, maiúscula, minúscula, número e símbolo), gravar `senha_hash` (BCrypt) e `mfa_secret` com a conta pendente; devolver o `otpauthUri` (o frontend gera o QR code). (2) Só ativar a conta quando o código TOTP for confirmado. Contas pendentes expiram (ex.: 30 min).
- Ao ativar: criar o `investidor` (perfil `INVESTIDOR`), a `conta` com **saldo inicial simulado** (o frontend assume R$ 10.000) e a linha em `limite_risco` com os limites padrão. Tudo na mesma transação. Perfis `AUDITOR` e `ADMIN` **nunca** são criados por essa rota.
- Registrar na auditoria: `USUARIO_CRIADO`.
- Perfis `INVESTIDOR`, `AUDITOR`, `ADMIN` com `@PreAuthorize`. Invalidar token no logout.
- Registrar na auditoria: `LOGIN_OK`, `LOGIN_FALHA`, `MFA_FALHA`.

### Conta e carteira (`conta`) · RF02
- Saldo `disponível` e `bloqueado`; posições por ativo com **preço médio** recalculado a cada compra.
- Cálculos de patrimônio, valor atual e resultado por posição.
- **Reserva de saldo:** ao aceitar uma compra, mover o valor de disponível para bloqueado; liberar na execução (devolvendo a diferença de preço) ou no cancelamento.
- Operações de saldo e posição **atômicas** (transação) e com **controle de concorrência** (lock pessimista ou `@Version`), para impedir saldo negativo em ordens simultâneas.

### Ativos e cotações (`ativo`) · RF03
- Cadastro de ativos (ação, ETF, FII) e status de mercado `ABERTO/FECHADO`.
- **Provedor de cotações simulado:** um agendador (`@Scheduled`) gera variações de preço, atualiza `ativo.preco_atual` e grava `cotacao_historico`.
- Cache da última cotação; endpoint de série histórica (30 pontos); publicação das atualizações para o BFF.
- Cotação defasada (provedor fora do ar) deve ser sinalizada e **bloquear ordens a mercado**.

### Ordens (`ordem`) · RF04, RF05, RF06, RNF02
- Criar, consultar, listar com filtros e cancelar.
- **Máquina de estados** com transições válidas apenas:
  `PENDENTE → VALIDADA → ENVIADA_BOLSA → (PARCIALMENTE_EXECUTADA →) EXECUTADA`; `PENDENTE/VALIDADA/ENVIADA_BOLSA → CANCELADA`; `PENDENTE → REJEITADA`. Qualquer outra transição é erro.
- Cada mudança de estado grava um **evento** (status, instante, detalhe) que alimenta a linha do tempo da tela.
- **Idempotência:** `Idempotency-Key` único (coluna `UNIQUE`); repetir a chave devolve a mesma ordem, sem criar outra. Se a mesma chave vier com corpo diferente, responder `422`.
- Sempre **registrar a ordem**, mesmo quando rejeitada, com `motivoRejeicao` exato.

### Validação de risco (`ordem.ValidadorRisco`) · RF05, RN01, RN03
Antes de enviar à bolsa, nesta ordem, devolvendo o motivo exato do primeiro que falhar:
1. Ativo existe e **mercado aberto** (RN03).
2. **Limite por ordem** e **limite diário** (valor), e **quantidade máxima de ordens por dia**.
3. **Compra:** saldo disponível ≥ quantidade × preço limite.
4. **Venda:** posição disponível ≥ quantidade (descontando vendas já abertas).
5. Dados válidos (quantidade inteira > 0, preço > 0).

### Bolsa simulada (`bolsa`) · RF06
- Recebe a ordem e responde de forma assíncrona: executada total, **executada parcialmente**, rejeitada, ou lenta/falha de propósito (para testar a recuperação).
- Preço de execução respeitando o limite (compra: o menor entre cotação e limite; venda: o maior).
- Ao executar: liquidar saldo e posição, atualizar `quantidadeExecutada`/`precoExecucao`, mudar o estado.

### Mensageria (`mensageria`)
- Fila entre "ordem validada" e "envio à bolsa" (e para notificações/auditoria).
- **Retentativa com backoff**, **fila de mensagens mortas (DLQ)** e consumidores **idempotentes** (processar a mesma mensagem duas vezes não pode duplicar a execução).
- Para o protótipo, uma fila em memória por trás de uma interface já cumpre, se o RabbitMQ pesar.

### Auditoria (`auditoria`) · RF08, RN02
- **Somente inserção.** O `schema.sql` já traz a cadeia de hashes e os triggers que bloqueiam `UPDATE/DELETE/TRUNCATE`.
- Registrar: login, falhas, criação/mudança de estado/cancelamento de ordem, alteração de limites, de mercado e do modo seguro (quem, quando, o quê).
- `GET /auditoria/integridade`: percorrer a cadeia recomputando cada hash e devolver `{ok, verificados, falhaEm}`.
- O usuário do banco usado pela aplicação não deve ter permissão de `UPDATE/DELETE` nessa tabela.

### Notificações (`notificacao`) · RF07
- Criar uma notificação a cada **execução, rejeição, cancelamento ou falha** de ordem (consumindo os eventos).
- Listar as do usuário e marcar como lida. (Envio por e-mail/push pode ser simulado em log.)

### Administração e modo seguro
- Alterar limites por investidor, abrir/fechar mercado por ativo (cada ação auditada).
- **Modo seguro:** quando ativo, `POST /ordens` responde `503`; consultas e ordens já enviadas continuam funcionando. Deve sobreviver a reinício (guardar no banco).

### Transversal
- Validação de entrada (`@Valid`), `GlobalExceptionHandler` com respostas padronizadas e sem vazar detalhes internos.
- Segredos só por variável de ambiente (`.env`), nunca no código.
- Consultas parametrizadas (JPA) contra SQL injection; sem logar dados sensíveis.
- `/actuator/health` para monitoramento; logs com correlation-id.
- **Recuperação:** ao reiniciar, retomar ordens `ENVIADA_BOLSA` sem desfecho (reconciliar com a bolsa) e reprocessar a fila.

## 4.1 Alternativa: Keycloak (opcional, não adotada)
O Keycloak é um servidor de identidade pronto (cadastro, login, MFA TOTP, recuperação de senha, bloqueio por tentativas, perfis, tokens OIDC). **Não é obrigatório** para este projeto; a decisão atual é manter a autenticação no próprio backend. Se a equipe quiser adotá-lo:
- Adicionar o serviço `keycloak` ao `docker-compose.yml`, criar um *realm* `sentineltrade` com os papéis `INVESTIDOR`, `AUDITOR`, `ADMIN` e TOTP como ação obrigatória. Exportar o realm para o repositório (sem segredos).
- Backend e BFF viram *resource servers*: só validam o JWT do Keycloak (`spring-boot-starter-oauth2-resource-server`), sem emitir token.
- O login passa a ser por redirecionamento (OIDC, fluxo *authorization code* com PKCE). As telas de login, MFA, cadastro e recuperação de senha do frontend seriam **substituídas pelas páginas do Keycloak**, que podem ser personalizadas com o tema do projeto.
- A criação do `investidor`/`conta`/limites continuaria no backend (disparada no primeiro acesso ou por evento do Keycloak). As colunas `senha_hash` e `mfa_secret` deixam de ser usadas.
- Atenção: o `Implantacao.md` pede "MFA simulado" e a RF01 descreve a autenticação própria. Trocar por Keycloak exigiria ajustar esses textos e a matriz de rastreabilidade.

## 5. Testes que o enunciado cobra (`backend/Implantacao.md`)
- **Transições de estado** da ordem: todas as válidas passam e as inválidas falham.
- **Validação de risco:** saldo insuficiente, posição insuficiente, limite por ordem, limite diário, mercado fechado.
- **Idempotência:** mesma chave duas vezes gera uma única ordem.
- **Auditoria:** UPDATE/DELETE em `auditoria_log` falham; cadeia íntegra detectada e adulteração detectada.
- **Autenticação:** senha errada, MFA errado, bloqueio por tentativas, acesso de perfil errado (403).
- **Cadastro:** senha fraca recusada, e-mail duplicado (409), conta só ativa após o código MFA, código errado não ativa.
- Os dados do `database/seed.sql` já foram pensados para esses casos (Ana com R$ 5.000 e 100 PETR4; MGLU3 com mercado fechado).

## 6. Esteira DevSecOps sugerida (GitHub Actions)
Build + testes do backend, BFF e frontend · varredura de segredos (gitleaks) · análise de dependências (OWASP Dependency-Check) · análise estática (CodeQL) · falhar o pipeline se algo crítico aparecer.

## 7. Ordem de implementação sugerida
1. Entidades JPA + repositórios (a partir do `schema.sql`).
2. Autenticação com JWT, depois o MFA.
3. Ativos e provedor de cotações simulado.
4. Ordens: máquina de estados + validador de risco + idempotência (com testes).
5. Bolsa simulada + fila + execução.
6. Auditoria e notificações.
7. BFF: contrato da seção 2, começando por login e ativos.
8. Ligar o frontend (`VITE_USE_MOCK=false`) e ajustar o que divergir.

## 8. Decisões que a equipe precisa tomar
- BFF em Spring Boot (recomendado) ou Node.
- Token em cookie `httpOnly` (recomendado) ou Bearer.
- Fila real (RabbitMQ) ou em memória no protótipo.
- Tempo real das cotações: SSE (já assumido no frontend) ou polling.