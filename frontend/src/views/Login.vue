<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../services/api'
import { session } from '../store/session'

const route = useRoute(), router = useRouter()
const email = ref(''), senha = ref(''), erro = ref(''), carregando = ref(false)
const demo = import.meta.env.VITE_USE_MOCK !== 'false'

async function entrar() {
  erro.value = ''; carregando.value = true
  try {
    const r = await api.login(email.value.trim(), senha.value)
    session.mfaToken = r.mfaToken; session.email = email.value.trim()
    router.push({ name: 'mfa', query: { redirect: route.query.redirect } })
  } catch (e) { erro.value = e.message } finally { carregando.value = false }
}
</script>

<template>
  <div class="auth-wrap">
    <RouterLink to="/" class="auth-logo">Sentinel Trade</RouterLink>
    <div class="auth-main">
      <form class="auth-card" @submit.prevent="entrar">
        <div v-if="route.query.expirou" class="alert warn">Sua sessão expirou. Entre novamente.</div>
        <label for="email">Email:</label>
        <input id="email" v-model="email" type="email" class="input" autocomplete="username" required />
        <label for="senha">Senha:</label>
        <input id="senha" v-model="senha" type="password" class="input" autocomplete="current-password" required />
        <div style="text-align:right;margin-top:.4rem"><RouterLink to="/esqueci-senha" class="link">Esqueci a senha</RouterLink></div>
        <div v-if="erro" class="alert err">{{ erro }}</div>
        <button class="btn" :disabled="carregando">{{ carregando ? 'Entrando…' : 'Entrar' }}</button>
        <div style="text-align:center;margin-top:.8rem"><RouterLink to="/cadastro" class="link">Não tem conta? Criar conta</RouterLink></div>
        <p v-if="demo" class="demo">Modo demonstração: ana@orion.test · bruno@orion.test · auditor@orion.test · admin@orion.test<br />Senha: Teste@123</p>
      </form>
    </div>
  </div>
</template>
