# Projeto de Projeto de Software - SentinelTrade

## Integrantes:

* Débora Lobato Santos / RA: 10732810
* Helen Santana de Araújo Teixeira / RA: 10742524
* João Pedro Mazzante Alvarez / RA: 10723837
* Vinicius Bisordi Acauã / RA: 10739883
---

## Visão do Projeto

O SentinelTrade tem o intuito de unificar a gestão de contas, carteiras, cotações em tempo real e roteamento de ordens da Orion Capital, substituindo sistemas legados pouco integrados. O foco central é garantir rastreabilidade de ordens, controle rigoroso de risco, auditoria imutável e alta disponibilidade, mitigando impactos financeiros, regulatórios e reputacionais.

---

## Tecnologias

- Backend:
- Frontend:
- BFF:
- Banco de Dados:
- Infraestrutura:

---

## Instruções de Execução

Como o projeto está em fase inicial de especificação e modelagem, os serviços de código ainda serão implementados. Para clonar o repositório e preparar o ambiente base:

1. Clone o Repositório
```
git clone <url-do-seu-repositorio>
cd SentinelTrade
```

2. Crie o arquivo de variáveis de ambiente com base no exemplo:
```
cp .env.example .env
```

3. Suba a infraestrutura base (quando os containers estiverem configurados):
```
docker-compose up -d
```
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
