# BFF - SentinelTrade

Backend For Frontend: a camada entre o `frontend/` (Vue) e o `backend/` (Spring Boot).

```
Vue (5173) ──HTTP/HTTPS──▶ BFF (3000) ──HTTP interno──▶ Backend (8080)
```

**Regra de ouro:** o BFF **não decide nada de negócio** (saldo, risco, estados da ordem). Ele **traduz, agrega e protege**. O backend revalida tudo e nunca confia no BFF.

Tecnologia: **Java 21 + Spring Boot 3.5** (Spring Web, `RestClient`, `SseEmitter`, Bean Validation). Nenhuma biblioteca extra.

---

## 1. Como rodar (passo a passo)

### Pré-requisitos

Os mesmos do backend: **JDK 21** e **Maven 3.9+**. Para conferir, abra o terminal e rode:

```bash
java -version
mvn -version
```

As duas linhas precisam mostrar a versão 21 do Java.

### Passo 1: configurar o `.env`

Na **raiz** do projeto (pasta `Projeto-de-SentinelTrade`), se ainda não existir o `.env`:

```bash
cp .env.example .env
```

Abra o `.env` e preencha `BFF_INTERNAL_KEY` com uma senha longa qualquer (ela precisa ser **igual** no backend). As outras variáveis do BFF já vêm com valores bons para rodar na sua máquina.

### Passo 2: subir o BFF

```bash
cd bff
mvn spring-boot:run
```

A primeira vez demora porque o Maven baixa as bibliotecas. Quando aparecer `Started BffApplication`, está no ar.

### Passo 3: conferir

Abra no navegador: http://localhost:3000/actuator/health. Deve aparecer `{"status":"UP"}`.

### Passo 4: rodar os testes

```bash
cd bff
mvn test
```

Os testes **não precisam do backend nem do banco**: um deles sobe um "backend falso" para testar o BFF de ponta a ponta.

### Passo 5: ligar o frontend ao BFF

1. Na pasta `frontend/`, copie o arquivo de exemplo: `cp .env.example .env`
2. No `frontend/.env`, troque `VITE_USE_MOCK=true` por `VITE_USE_MOCK=false`
3. Rode o frontend normalmente (`npm run dev`)

> Atenção: o login e as telas só vão funcionar de verdade quando o **backend** expuser as rotas da seção 3. Enquanto isso, mantenha `VITE_USE_MOCK=true` para usar o frontend sozinho.

Também dá para subir tudo junto da raiz com `make` (banco, backend, BFF e frontend).

---

## 2. Rotas que o BFF expõe para o frontend

É exatamente o contrato do [`OQ_PRECISA.md`](../OQ_PRECISA.md), seção 2. Todos os erros saem como `{ "message": "texto para o usuário" }`.

| Rota | Perfil | O que o BFF faz além de repassar |
|---|---|---|
| `POST /api/auth/login` | público | Rate limit rigoroso |
| `POST /api/auth/mfa` | público | **Cria a sessão**: guarda o JWT do backend e devolve um token opaco + cookie `httpOnly` |
| `POST /api/auth/cadastro`, `/cadastro/mfa` | público | Valida formato |
| `POST /api/auth/esqueci-senha` | público | Sempre a mesma resposta (`202`), exista ou não o e-mail |
| `POST /api/auth/logout` | logado | Avisa o backend, apaga a sessão e o cookie |
| `GET /api/dashboard` | INVESTIDOR | **Agrega** carteira + ordens + ativos em uma resposta |
| `GET /api/carteira`, `/api/ativos`, `/api/ativos/:ticker` | INVESTIDOR | |
| `GET /api/cotacoes/stream` | INVESTIDOR | **SSE**: uma consulta ao backend, reenviada a todos os navegadores |
| `POST /api/ordens` | INVESTIDOR | Exige `Idempotency-Key` e repassa **sem alterar** |
| `GET /api/ordens`, `GET/DELETE /api/ordens/:id` | INVESTIDOR | Valida filtros e id |
| `GET /api/notificacoes`, `PATCH /api/notificacoes/:id/lida` | INVESTIDOR | |
| `GET /api/auditoria`, `/api/auditoria/integridade` | AUDITOR, ADMIN | |
| `GET /api/admin/estado`, `PUT /api/admin/...` | ADMIN | |

---

## 3. O que o backend precisa expor para o BFF (contrato interno)

Para simplificar, o backend usa **as mesmas rotas e os mesmos formatos** da seção 2, também com prefixo `/api` (ex.: o BFF recebe `GET /api/carteira` e chama `GET http://localhost:8080/api/carteira`). As diferenças:

1. **`POST /api/auth/mfa` do backend** devolve o JWT real: `{ "token": "<JWT>", "usuario": { "id", "nome", "email", "perfil" } }`. O BFF guarda esse JWT e **nunca** o entrega ao navegador.
2. **Autenticação:** nas rotas protegidas, o BFF envia `Authorization: Bearer <JWT do backend>`.
3. **`GET /internal/cotacoes`** (rota nova, só para o BFF): devolve a lista de ativos no mesmo formato de `GET /api/ativos`. Não usa JWT; o backend deve aceitar **somente** se o cabeçalho `X-Bff-Key` for igual a `BFF_INTERNAL_KEY`.
4. **`GET /api/dashboard` não existe no backend**: o BFF monta a resposta a partir de `/api/carteira`, `/api/ordens?aba=todas` e `/api/ativos`.
5. **Erros:** o backend pode responder `{ "message": ... }` ou `{ "mensagem": ... }`; o BFF entende os dois e sempre devolve `message`. Os status `400, 401, 403, 404, 409, 422, 429, 503` passam direto. Qualquer outro `5xx` vira `502` com uma mensagem genérica (nada de detalhe interno).

Cabeçalhos que o BFF envia em **toda** chamada (para a auditoria):

| Cabeçalho | Conteúdo |
|---|---|
| `X-Correlation-Id` | Id da requisição (o mesmo aparece nos logs do BFF) |
| `X-Forwarded-For` | IP do cliente |
| `X-Client-User-Agent` | Navegador do cliente (máx. 256 caracteres) |
| `X-Bff-Key` | Chave interna, prova de que a chamada veio do BFF |
| `Idempotency-Key` | Só em `POST /api/ordens`, exatamente como veio do frontend |

---

## 4. Segurança e resiliência implementadas

Itens da seção 3 do `OQ_PRECISA.md`:

| # | Item | Onde está |
|---|---|---|
| 1 | Contrato da seção 2 | `api/*Controller.java` |
| 2 | Sessão e token: JWT fica só no BFF; navegador recebe token opaco (cookie `httpOnly; SameSite=Strict` + Bearer). Expira com 30 min sem uso ou 8 h no total | `sessao/` |
| 3 | Agregação do dashboard | `dashboard/DashboardMontador.java` |
| 4 | Cotações por SSE com *fan-out* | `cotacoes/CotacoesService.java` |
| 5 | `Idempotency-Key` repassado sem alteração | `InvestidorController.criarOrdem` |
| 6 | Autorização por perfil (primeira barreira) | `sessao/Autenticacao.java` |
| 7 | Validação de entradas (tipos, tamanhos, ticker, números positivos, campos desconhecidos recusados, `1.5` não vira `1`) | `api/Dtos.java`, `comum/Validacoes.java`, `application.yml` |
| 8 | CORS restrito, rate limit (20/min no login, 300/min no resto, por IP), corpo de no máximo 16 KB, cabeçalhos de segurança (CSP, HSTS, nosniff...), timeout (2 s conexão, 5 s leitura) e *circuit breaker* | `config/BffConfig.java`, `filtros/`, `backend/` |
| 9 | Erros normalizados para `{message}`, sem stack trace | `comum/ErroHandler.java`, `backend/BackendClient.java` |
| 10 | Correlation-id gerado e propagado; IP e user-agent repassados | `filtros/CorrelationIdFilter.java`, `BackendClient` |
| 11 | Modo seguro: `503` do backend volta como `503` | `BackendClient` |
| 12 | Logs sem dados sensíveis (só método, caminho, status e tempo) | `CorrelationIdFilter` |
| 13 | Spring Boot na pasta `bff/` | este projeto |

O *circuit breaker* abre depois de 5 falhas seguidas do backend (erro de rede, timeout ou 5xx). Nesse caso, o BFF responde `503` na hora por 30 s, sem sobrecarregar o backend, e depois testa de novo com uma única chamada.

---

## 5. Estrutura

```text
bff/
├── pom.xml
└── src/
    ├── main/java/br/sentineltrade/bff/
    │   ├── BffApplication.java
    │   ├── api/          # Controllers (contrato com o frontend) e DTOs validados
    │   ├── backend/      # BackendClient (chamadas ao backend) e CircuitBreaker
    │   ├── comum/        # Erros, validação de parâmetros, utilitários
    │   ├── config/       # Propriedades, beans e filtros
    │   ├── cotacoes/     # SSE de cotações
    │   ├── dashboard/    # Agregação do GET /dashboard
    │   ├── filtros/      # Correlation-id, cabeçalhos de segurança, rate limit
    │   ├── seguranca/    # Limitador de taxa
    │   └── sessao/       # Sessões, usuário e checagem de perfil
    ├── main/resources/application.yml
    └── test/java/...     # Testes unitários + teste de integração com backend falso
```

---

## 6. Limitações conhecidas (protótipo)

- **Sessões e rate limit ficam em memória:** reiniciar o BFF desloga todo mundo, e não dá para rodar duas instâncias. Para produção, trocar por Redis.
- **HTTPS:** em produção, o BFF deve ficar atrás de um proxy com TLS, com `COOKIE_SECURE=true` e `TRUST_PROXY=true`.
- **Atrás do proxy do Vite** (`npm run dev`), todas as requisições chegam do mesmo IP (`127.0.0.1`), então o rate limit vale para o grupo todo de uma vez.
