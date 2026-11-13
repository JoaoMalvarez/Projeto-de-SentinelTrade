<script setup>
  import { brl, pct, cor } from '../utils'

  // Dados de EXEMPLO: antes do login a landing não consegue ler as cotações reais.
  const amostra = [
    { ticker: 'PETR4', preco: 38.5, variacao: 1.05 },
    { ticker: 'VALE3', preco: 62.1, variacao: 1.14 },
    { ticker: 'ITUB4', preco: 33.75, variacao: -0.44 },
    { ticker: 'BOVA11', preco: 128.4, variacao: 0.94 },
    { ticker: 'IVVB11', preco: 305.2, variacao: -0.42 },
    { ticker: 'HGLG11', preco: 160.9, variacao: 0.5 },
    { ticker: 'MXRF11', preco: 10.35, variacao: 0.49 }
  ]
  const faixa = [...amostra, ...amostra] // duplicada para o loop não ter emenda

  const destaques = [
    { ticker: 'PETR4', nome: 'Petrobras PN', preco: 38.5, variacao: 1.05, serie: [37.6, 37.8, 37.7, 38.0, 38.2, 38.1, 38.3, 38.2, 38.4, 38.3, 38.6, 38.5] },
    { ticker: 'VALE3', nome: 'Vale ON', preco: 62.1, variacao: 1.14, serie: [60.9, 61.2, 61.0, 61.5, 61.4, 61.8, 61.6, 62.0, 61.9, 62.2, 62.0, 62.1] },
    { ticker: 'BOVA11', nome: 'ETF Ibovespa', preco: 128.4, variacao: 0.94, serie: [126.5, 126.9, 127.1, 126.8, 127.4, 127.2, 127.8, 127.6, 128.0, 128.1, 128.3, 128.4] },
    { ticker: 'HGLG11', nome: 'CSHG Logística FII', preco: 160.9, variacao: 0.5, serie: [159.8, 160.0, 159.9, 160.3, 160.2, 160.5, 160.4, 160.7, 160.6, 160.8, 160.7, 160.9] }
  ]
  const pontos = (s) => {
    const min = Math.min(...s), max = Math.max(...s)
    return s.map((v, i) => `${(i / (s.length - 1)) * 100},${40 - ((v - min) / (max - min || 1)) * 36 - 2}`).join(' ')
  }

  const numeros = [
    { n: '8', t: 'ativos simulados' },
    { n: '2', t: 'etapas de verificação (senha + MFA)' },
    { n: '0', t: 'ordens duplicadas' },
    { n: '100%', t: 'dos eventos auditados' }
  ]
  const passos = [
    { t: 'Crie sua conta', d: 'Cadastro rápido, com senha forte.' },
    { t: 'Ative o MFA', d: 'Escaneie o QR code no app autenticador.' },
    { t: 'Acompanhe as cotações', d: 'Preços em tempo quase real, com busca e filtros.' },
    { t: 'Envie suas ordens', d: 'Compra e venda validadas e auditadas.' }
  ]
  const perguntas = [
    { q: 'É dinheiro de verdade?', a: 'Não. O Sentinel Trade é um simulador acadêmico: ativos, contas e cotações são fictícios e não há conexão com bolsas reais.' },
    { q: 'Como funciona o MFA?', a: 'Depois da senha, você informa um código de 6 dígitos gerado por um app autenticador (como Google Authenticator), configurado no cadastro.' },
    { q: 'O que acontece se minha ordem for rejeitada?', a: 'Ela fica registrada com o motivo exato (saldo ou posição insuficiente, limite de risco ou mercado fechado) e você recebe uma notificação.' },
    { q: 'Posso enviar a mesma ordem duas vezes por engano?', a: 'Não. Cada envio carrega uma chave de idempotência: se o mesmo pedido chegar de novo, o sistema devolve a ordem original.' },
    { q: 'Os registros de auditoria podem ser alterados?', a: 'Não. A auditoria é somente de inserção e usa uma cadeia de hashes, então qualquer adulteração é detectada.' }
  ]
  const ano = new Date().getFullYear()
</script>

<template>
  <main>
    <section class="hero">
      <h1>SentinelTrade</h1>
      <p>Negocie ações, ETFs e fundos imobiliários com segurança, rastreabilidade e auditoria em cada ordem.</p>
      <RouterLink to="/cadastro" class="btn big">Comece Gratuitamente</RouterLink>
    </section>

    <div class="faixa" aria-label="Cotações de exemplo">
      <div class="faixa-trilho">
        <span v-for="(a, i) in faixa" :key="i" class="tick">
          <strong>{{ a.ticker }}</strong> {{ brl(a.preco) }}
          <span :class="cor(a.variacao)">{{ pct(a.variacao) }}</span>
        </span>
      </div>
    </div>

    <section class="section">
      <div class="stats">
        <div v-for="s in numeros" :key="s.t" class="stat card">
          <div class="n">{{ s.n }}</div><div class="t">{{ s.t }}</div>
        </div>
      </div>
    </section>

    <section id="como-funciona" class="section">
      <h2>Como funciona</h2>
      <div class="grid3">
        <div v-for="(p, i) in passos" :key="p.t" class="card passo">
          <div class="num">{{ i + 1 }}</div><h3>{{ p.t }}</h3><p class="muted">{{ p.d }}</p>
        </div>
      </div>
    </section>

    <section id="destaques" class="section">
      <div class="row between"><h2>Ativos em destaque</h2><span class="muted small">Dados de exemplo (simulados)</span></div>
      <div class="grid3">
        <RouterLink v-for="a in destaques" :key="a.ticker" to="/login" class="card dest">
          <h3>{{ a.ticker }}</h3><div class="muted small">{{ a.nome }}</div>
          <div class="preco">{{ brl(a.preco) }}</div>
          <div :class="cor(a.variacao)">{{ pct(a.variacao) }}</div>
          <svg viewBox="0 0 100 40" preserveAspectRatio="none" aria-hidden="true">
            <polyline :points="pontos(a.serie)" fill="none" :stroke="a.variacao >= 0 ? '#4ade80' : '#f87171'" stroke-width="2" vector-effect="non-scaling-stroke" />
          </svg>
        </RouterLink>
      </div>
    </section>

    <section id="produtos" class="section">
      <h2>Produtos</h2>
      <div class="grid3">
        <div class="card">
          <h3>Ações</h3>
          <p class="muted">Participe do capital das maiores empresas listadas.</p>
        </div>

        <div class="card">
          <h3>ETFs</h3>
          <p class="muted">Diversifique com um único ativo que replica um índice.</p>
        </div>

        <div class="card">
          <h3>Fundos imobiliários</h3>
          <p class="muted">Receba renda recorrente de imóveis e recebíveis.</p>
        </div>

        <div class="card">
          <h3>Renda Fixa</h3>
          <p class="muted">Invista em títulos públicos e privados com previsibilidade de rendimento e proteção contra a volatilidade do mercado.</p>
        </div>

        <div class="card">
          <h3>Opções</h3>
          <p class="muted">Proteja sua carteira ou alavanque posições utilizando contratos derivativos de compra e venda com prazos definidos.</p>
        </div>

        <div class="card">
          <h3>Fundos de Investimento</h3>
          <p class="muted">Aplique em carteiras gerenciadas por gestores profissionais com diferentes perfis de risco e estratégias globais.</p>
        </div>

        <div class="card">
          <h3>Criptomoedas</h3>
          <p class="muted">Negocie ativos digitais descentralizados com execução ágil e custódia integrada à sua carteira.</p>
        </div>

        <div class="card">
          <h3>Mercado Futuro</h3>
          <p class="muted">Negocie contratos de commodities, índices e moedas com foco em hedge ou especulação direcional.</p>
        </div>
      </div>
    </section>

    <section id="mercados" class="section">
      <h2>Mercados</h2>
      <p class="muted">Cotações em tempo quase real, validação de saldo, posição e limites de risco antes de cada ordem, e acompanhamento completo do ciclo de vida: da criação à execução.</p>
      <p class="muted">○ Foco em baixa latência e precisão:</p>
      <div class="destaque-lateral">
      <p class="muted1">Motor de matching determinístico, verificação pré-trade de margem e saldo em milissegundos, e rastreabilidade total de cada ordem do envio à liquidação.</p>
      </div>
      <p class="muted">○ Foco em experiência e controle:</p>
      <div class="destaque-lateral">
      <p class="muted1">Livro de ofertas atualizado continuamente, checagem automatizada de limites operacionais e histórico detalhado de estados: pendente, executada ou cancelada.</p>
      </div>
      <p class="muted">○ Foco em arquitetura e confiabilidade:</p>
      <div class="destaque-lateral">
      <p class="muted1">Infraestrutura orientada a eventos com validação rigorosa de risco antes da roteirização e visibilidade completa sobre o ciclo de vida das operações.</p>
      </div>
    </section>

    <section id="corretoras" class="section">
      <h2>Corretoras</h2>
      <p class="muted">A Orion Capital é um simulador educacional de negociação eletrônica. Nenhuma transação possui valor monetário real ou conexão com bolsas oficiais.</p>
    </section>

        <section id="ot" class="section">
      <h2>Operação & Trading</h2>
      <div class="grid3">
        <div class="card">
          <h3>Ordens a mercado e limite</h3>
          <p class="muted">Escolha entre execução imediata ao melhor preço ou definição exata do valor de entrada e saída.</p>
        </div>

        <div class="card">
          <h3>Cálculo dinâmico de P&L</h3>
          <p class="muted">Acompanhamento instantâneo de lucro, prejuízo e preço médio ajustado a cada negociação.</p>
        </div>

        <div class="card">
          <h3>Gestão de risco integrada</h3>
          <p class="muted">Definição nativa de Stop Loss e Take Profit vinculados diretamente à ordem principal.</p>
        </div>

        <div class="card">
          <h3>Extrato detalhado</h3>
          <p class="muted">Histórico completo de movimentações, taxas simuladas e liquidações por ativo.</p>
        </div>
      </div>
    </section>

    <section id="mais" class="section">
      <h2>Mais</h2>
      <div class="grid3">
        <div class="card">
          <h3>Login com MFA</h3>
          <p class="muted">Senha e segundo fator em todo acesso.</p>
        </div>

        <div class="card">
          <h3>Auditoria imutável</h3>
          <p class="muted">Cada evento fica registrado em uma cadeia de hashes.</p>
        </div>

        <div class="card">
          <h3>Sem ordens duplicadas</h3>
          <p class="muted">Chave de idempotência protege contra reenvios.</p>
        </div>

        <div class="card">
          <h3>Livro de ofertas em tempo real</h3>
          <p class="muted">Sincronização via WebSockets com baixa latência para atualização imediata de preços e liquidez.</p>
        </div>

        <div class="card">
          <h3>Validação pré-trade</h3>
          <p class="muted">Bloqueio automático de ordens que excedam o saldo disponível ou os limites de exposição definidos.</p>
        </div>

        <div class="card">
          <h3>Isolamento de sessões</h3>
          <p class="muted">Tokens de curta duração e revogação instantânea para proteger operações ativas.</p>
        </div>

        <div class="card">
          <h3>Consistência transacional</h3>
          <p class="muted">Operações atômicas garantem que saldos e posições nunca fiquem em estados intermediários inválidos.</p>
        </div>

        <div class="card">
          <h3>Telemetria e observabilidade</h3>
          <p class="muted">Métricas de latência, taxa de execução e logs estruturados para monitoramento contínuo do sistema.</p>
        </div>

        <div class="card">
          <h3>Controle de concorrência</h3>
          <p class="muted">Locks otimistas evitam condições de corrida durante atualizações simultâneas de carteira.</p>
        </div>

        <div class="card">
          <h3>Carteira de Dividendos</h3>
          <p class="muted">Acompanhe os proventos gerados pelos seus ativos e o histórico de distribuição de rendimentos.</p>
        </div>

        <div class="card">
          <h3>Simulador de Alocação</h3>
          <p class="muted">Teste estratégias de diversificação e veja o balanceamento ideal entre renda variável e fundos.</p>
        </div>

        <div class="card">
          <h3>Histórico de Liquidez</h3>
          <p class="muted">Monitore o volume de negociação e a profundidade de mercado disponível para cada instrumento financeiro.</p>
        </div>
      </div>
    </section>

    <section id="faq" class="section">
      <h2>Perguntas frequentes</h2>
      <div class="faq">
        <details v-for="p in perguntas" :key="p.q"><summary>{{ p.q }}</summary><p>{{ p.a }}</p></details>
      </div>
    </section>

    <section class="section">
      <div class="cta">
        <h2>Pronto para começar?</h2>
        <p class="muted">Crie sua conta simulada em menos de um minuto.</p>
        <div class="row">
          <RouterLink to="/cadastro" class="btn big">Criar conta gratuita</RouterLink>
          <RouterLink to="/login" class="btn ghost big">Já tenho conta</RouterLink>
        </div>
      </div>
    </section>

    <footer class="rodape">
      <div class="rodape-in">
        <div>
          <strong>Sentinel Trade</strong>
          <p class="muted small">Projeto acadêmico. A Orion Capital é fictícia e nenhum dinheiro real é movimentado.</p>
        </div>
        <div>
          <strong>Plataforma</strong>
          <p class="small">
            <RouterLink to="/login">Entrar</RouterLink><br />
            <RouterLink to="/cadastro">Criar conta</RouterLink><br />
            <RouterLink :to="{ path: '/', hash: '#faq' }">Perguntas frequentes</RouterLink>
          </p>
        </div>
        <div>
          <strong>Equipe</strong>
          <p class="muted small">João Pedro Mazzante Alvarez</p>
        </div>
      </div>
      <p class="muted small" style="text-align:center;margin:1.5rem 0 0">© {{ ano }} Sentinel Trade · Dados simulados</p>
    </footer>

  </main>
</template>
