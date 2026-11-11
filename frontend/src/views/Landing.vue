<template>
  <main>
    <section class="hero">
      <h1>Sentinel Trade</h1>
      <p>Negocie ações, ETFs e fundos imobiliários com segurança, rastreabilidade e auditoria em cada ordem.</p>
      <RouterLink to="/cadastro" class="btn big">Comece Gratuitamente</RouterLink>
    </section>

    <section id="produtos" class="section">
      <h2>Produtos</h2>
      <div class="grid3">
        <div class="card"><h3>Ações</h3><p class="muted">Participe do capital das maiores empresas listadas.</p></div>
        <div class="card"><h3>ETFs</h3><p class="muted">Diversifique com um único ativo que replica um índice.</p></div>
        <div class="card"><h3>Fundos imobiliários</h3><p class="muted">Receba renda recorrente de imóveis e recebíveis.</p></div>
      </div>
    </section>

    <section id="mercados" class="section">
      <h2>Mercados</h2>
      <p class="muted">Cotações em tempo quase real, validação de saldo, posição e limites de risco antes de cada ordem, e acompanhamento completo do ciclo de vida: da criação à execução.</p>
    </section>

    <section id="corretoras" class="section">
      <h2>Corretoras</h2>
      <p class="muted">A Orion Capital é uma corretora fictícia, criada para este projeto acadêmico. Todos os ativos, contas e cotações são simulados e nenhum dinheiro real é movimentado.</p>
    </section>

    <section id="mais" class="section">
      <h2>Mais</h2>
      <div class="grid3">
        <div class="card"><h3>Login com MFA</h3><p class="muted">Senha e segundo fator em todo acesso.</p></div>
        <div class="card"><h3>Auditoria imutável</h3><p class="muted">Cada evento fica registrado em uma cadeia de hashes.</p></div>
        <div class="card"><h3>Sem ordens duplicadas</h3><p class="muted">Chave de idempotência protege contra reenvios.</p></div>
      </div>
    </section>
  </main>
</template>
