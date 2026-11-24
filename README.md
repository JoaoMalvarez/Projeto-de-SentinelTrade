# Projeto de Projeto de Software - SentinelTrade

## Integrantes:

* Débora Lobato Santos / RA: 10732810
* Helen Santana de Araújo Teixeira / RA: 10742524
* João Pedro Mazzante Alvarez / RA: 10723837
* Vinicius Bisordi Acauã / RA: 10739883
---

## Visão do Projeto

O SentinelTrade nasce da necessidade preta de modernizar e blindar a infraestrutura operacional da corretora fictícia Orion Capital. Atualmente, o ecossistema da corretora sofre com sistemas legados fragmentados, o que dificulta o rastreamento rigoroso de ordens, expõe a instituição a falhas de conformidade e torna o controle de risco e a auditoria processos lentos e suscetíveis a erros.

No mercado financeiro, falhas operacionais não representam apenas perdas técnicas; elas geram impactos diretos em três frentes críticas: financeira (perdas por execuções incorretas ou atrasadas), regulatória (penalidades por descumprimento de normas de conformidade) e reputacional (perda de confiança dos investidores). Para mitigar esses riscos, o SentinelTrade foi concebido como uma plataforma de trade financeiro distribuída, altamente segura, escalável e tolerante a falhas.

## 📂 Estrutura do Repositório

O projeto está organizado da seguinte forma:

```text
Projeto-de-SentinelTrade/
├── .env.example                 # Exemplo de variáveis de ambiente (copie para .env)
├── .gitignore
├── docker-compose.yml           # PostgreSQL local (roda schema.sql e seed.sql)
├── README.md
├── backend/                     # Spring Boot (Java 21) - monolito modular
│   ├── pom.xml
│   └── src/
│       ├── main/java/br/sentineltrade/
│       │   ├── auth/  conta/  ativo/  ordem/  bolsa/
│       │   ├── mensageria/  auditoria/  notificacao/  comum/
│       │   └── SentinelTradeApplication.java
│       ├── main/resources/application.yml
│       └── test/java/br/sentineltrade/
├── bff/                         # Backend For Frontend - Spring Boot (Java 21), porta 3000
├── frontend/                    # Vue 3 + Vite
├── database/                    # schema.sql, seed.sql, Rodar.md
└── docs/
    ├── diagramas/               # Casos de Uso, Máquina de Estados, Sequência
    └── requisitos/              # 01 a 04 + requisitos.md
```

## Tecnologias

- **Backend:** Java 21, Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator), Maven
- **Banco de dados:** PostgreSQL 16
- **Frontend:** Vue 3 + Vite
- **BFF:** Java 21, Spring Boot 3.5 (Web, Validation, Actuator) - ver [`bff/README.md`](bff/README.md)
- **Infraestrutura local:** Docker Compose

## Como executar

Pré-requisitos: JDK 21, Maven, Docker e Node.js 20+.

1. Configure as variáveis de ambiente (o `.env` nunca deve ser commitado):
   ```bash
   cp .env.example .env     # edite DB_PASSWORD
   ```
2. Suba o banco (executa `schema.sql` e `seed.sql` automaticamente):
   ```bash
   docker compose up -d
   ```
3. Rode o backend:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Teste em http://localhost:8080/actuator/health (deve retornar `UP`).
4. Rode o frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Abra http://localhost:5173

Mais detalhes sobre em [`Como-Rodar.md`](Como-Rodar.md).

### Pilares Arquiteturais e Funcionais
1. Segurança e Autenticação Rigorosa
A segurança é a camada inicial de defesa da plataforma. O acesso de investidores exige não apenas credenciais tradicionais com hash seguro, mas obrigatoriamente a verificação por Autenticação Multifator (MFA). Além disso, o controle de acesso por perfis garante que cada usuário interaja estritamente com seus próprios dados e limites financeiros, protegendo contra acessos não autorizados.

2. Ciclo de Vida e Validação de Ordens
Diferente de sistemas simplificados, o SentinelTrade implementa um rigoroso portão de risco pré-execução. Nenhuma ordem de compra ou venda gerada pelo investidor é enviada à Bolsa simulada sem que o sistema valide simultaneamente:

- A suficiência de saldo financeiro na conta;

- A posição real de ativos custodiados na carteira;

- Os limites de risco parametrizados para o perfil do investidor;

- A situação atual e a abertura do mercado para o ativo negociado.

Caso qualquer uma dessas validações falhe, a ordem é rejeitada preventivamente, gerando um registro imediato.

3. Resiliência, Mensageria e Prevenção de Duplicidade
Para garantir alta disponibilidade (alvo de 99,99%) e suportar picos de volatilidade sem queda de performance, a arquitetura utiliza processamento assíncrono baseado em filas de mensagens (mensageria).

- Idempotência: Para evitar que falhas de rede gerem o reenvio acidental de ordens duplicadas, o sistema utiliza identificadores únicos (UUID e chaves de idempotência) e cache distribuído, garantindo que a mesma ordem seja processada apenas uma vez, independentemente de quantas vezes o cliente tente reenviá-la em caso de timeout.

- Modo de Indisponibilidade Segura: Se houver instabilidade na comunicação com o provedor externo de cotações ou com a bolsa simulada, a plataforma entra em modo de proteção, enfileirando ou rejeitando operações com segurança para evitar exposições financeiras indesejadas.

4. Auditoria Imutável (Write Once, Read Many)
A conformidade regulatória exige que todas as transações, alterações de carteira e tentativas de login deixem uma trilha incorruptível. O SentinelTrade prevê o armazenamento de logs em estruturas imutáveis baseadas no conceito WORM (Write Once, Read Many). Isso impede que qualquer agente — interno ou externo — altere o histórico de operações, garantindo total transparência para auditorias regulatórias.

### Conclusão
Em suma, o SentinelTrade transforma a operação de trading da Orion Capital em um ambiente robusto, onde a velocidade das cotações em tempo quase real caminha lado a lado com a blindagem contra fraudes, duplicidades e falhas sistêmicas. O projeto entrega à banca avaliadora uma especificação completa de engenharia de software voltada para sistemas críticos onde a tolerância a falhas não é um diferencial, mas um requisito obrigatório.

---

## Requisitos 

### Engenharia de Requisitos 

- [X] Visão e objetivo do SentinelTrade
- [X] Identificação dos atores
- [X] Requisitos funcionais e não funcionais
- [X] Regras de negócio
- [X] Restrições técnicas
- [X] Critérios de aceitação
- [X] Matriz de rastreabilidade entre requisito, diagrama, implementação e teste

### Funcionais

- [ ] Cadastro e gestão de investidores
- [ ] Autenticação com MFA
- [ ] Consulta de carteira
- [ ] Consulta de cotações
- [ ] Envio de ordem de compra e venda
- [ ] Cancelamento de ordem
- [ ] Validação de saldo, posição e limites
- [ ] Acompanhamento do status da ordem
- [ ] Notificações; consulta de histórico
- [ ] Auditoria


### Segurança

- [ ] Senhas protegidas por hash
- [ ] MFA
- [ ] Controle de acesso por perfil
- [ ] Atributos privados
- [ ] Validação de entradas
- [ ] Tratamento seguro de exceções
- [ ] Prevenção de injeção
- [ ] Proteção contra reenvio/duplicidade de ordem
- [ ] Ausência de segredos no repositório

### Resiliência 

- [ ] Time-out de integração
- [ ] Retentativa controlada
- [ ] Idempotência
- [ ] Fila de mensagens para processamento assíncrono
- [ ] Modo de indisponibilidade segura
- [ ] Recuperação de falhas
- [ ] Consistência de dados

### Qualidade

- [ ] Auditabilidade
- [ ] Confidencialidade
- [ ] Integridade
- [ ] Disponibilidade
- [ ] Desempenho
- [ ] Rastreabilidade
- [ ] Manutenibilidade
