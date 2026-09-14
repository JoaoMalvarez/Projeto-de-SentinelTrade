# Projeto de Projeto de Software - SentinelTrade
O sistema deverá permitir que investidores autorizados acompanhem cotações, mantenham carteira de ativos, enviem ordens de compra e venda e consultem o histórico de operações. Decisões erradas, atrasadas ou não auditáveis podem gerar impacto relevante. Aqui, o impacto é financeiro, regulatório e reputacional.

O sistema deverá ser projetado como uma plataforma distribuída, segura, escalável e tolerante a falhas.

Contexto do problema
A corretora fictícia Orion Capital opera uma plataforma digital de negociação de ações, ETFs e fundos imobiliários. Atualmente, suas operações dependem de sistemas pouco integrados, dificultando a rastreabilidade das ordens, o controle de risco e a auditoria.

A Orion Capital contratou sua equipe para especificar e modelar o SentinelTrade, uma plataforma que deverá:

- autenticar usuários com MFA;
- manter dados de investidores, contas, carteiras, ativos e limites financeiros;
- receber cotações em tempo quase real por meio de um provedor externo;
- permitir ordens de compra, venda, cancelamento e consulta;
- validar saldo, posição em carteira, limite de risco e situação do mercado antes de transmitir a ordem;
- integrar-se a uma Bolsa/Corretora simulada;
- acompanhar o ciclo de vida das ordens;
- registrar logs de auditoria imutáveis;
- notificar o investidor sobre execução, rejeição, cancelamento ou falha;
- operar com mecanismos de recuperação, indisponibilidade controlada e prevenção de duplicidade de ordens.
- Não é necessário integrar com uma bolsa real ou utilizar dinheiro real. O projeto deverá operar com ativos, contas e cotações simuladas.
