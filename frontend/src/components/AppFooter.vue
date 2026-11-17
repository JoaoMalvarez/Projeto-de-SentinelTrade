<script setup>
import { session } from '../store/session'
const ano = new Date().getFullYear()
</script>

<template>
  <footer class="rodape">
    <div class="rodape-in">
      <div>
        <strong>Sentinel Trade</strong>
        <p class="muted small">Projeto acadêmico. A Orion Capital é fictícia e nenhum dinheiro real é movimentado.</p>
      </div>
      <div>
        <strong>Plataforma</strong>
        <p class="small">
          <template v-if="!session.token">
            <RouterLink to="/login">Entrar</RouterLink><br />
            <RouterLink to="/cadastro">Criar conta</RouterLink><br />
          </template>
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
</template>