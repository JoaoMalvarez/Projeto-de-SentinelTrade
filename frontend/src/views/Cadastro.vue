<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../services/api'
import { brl } from '../utils'

const router = useRouter()
const etapa = ref(1)
const f = ref({ nome: '', email: '', senha: '', confirmar: '' })
const termos = ref(false), erro = ref(''), carregando = ref(false)
const mfa = ref(null), token = ref(''), qr = ref(''), codigo = ref(''), saldo = ref(0)

const regras = computed(() => {
  const s = f.value.senha
  return [
    ['Mínimo de 8 caracteres', s.length >= 8],
    ['Letra maiúscula', /[A-Z]/.test(s)],
    ['Letra minúscula', /[a-z]/.test(s)],
    ['Número', /\d/.test(s)],
    ['Símbolo (ex.: @ # $ !)', /[^\w\s]/.test(s)],
    ['As senhas coincidem', !!s && s === f.value.confirmar]
  ]
})
const valido = computed(() => regras.value.every((r) => r[1]) && f.value.nome.trim().length >= 3 && f.value.email && termos.value)

async function criar() {
  erro.value = ''; carregando.value = true
  try {
    const r = await api.cadastrar({ nome: f.value.nome, email: f.value.email, senha: f.value.senha })
    token.value = r.cadastroToken; mfa.value = r.mfa
    const QRCode = (await import('qrcode')).default // carrega só quando necessário
    qr.value = await QRCode.toDataURL(r.mfa.otpauthUri, { margin: 1, width: 180 })
    etapa.value = 2
  } catch (e) { erro.value = e.message } finally { carregando.value = false }
}
async function confirmar() {
  erro.value = ''; carregando.value = true
  try { saldo.value = (await api.confirmarMfaCadastro(token.value, codigo.value)).saldoInicial || 0; etapa.value = 3 }
  catch (e) { erro.value = e.message; codigo.value = '' } finally { carregando.value = false }
}
</script>

<template>
  <div class="auth-wrap">
    <RouterLink to="/" class="auth-logo">Sentinel Trade</RouterLink>
    <div class="auth-main">
      <form v-if="etapa === 1" class="auth-card" @submit.prevent="criar">
        <p style="margin:0">Criar conta</p>
        <label for="nome">Nome completo:</label>
        <input id="nome" v-model="f.nome" class="input" autocomplete="name" required />
        <label for="email">Email:</label>
        <input id="email" v-model="f.email" type="email" class="input" autocomplete="email" required />
        <label for="senha">Senha:</label>
        <input id="senha" v-model="f.senha" type="password" class="input" autocomplete="new-password" required />
        <label for="conf">Confirmar senha:</label>
        <input id="conf" v-model="f.confirmar" type="password" class="input" autocomplete="new-password" required />
        <ul class="rules"><li v-for="r in regras" :key="r[0]" :class="{ ok: r[1] }">{{ r[0] }}</li></ul>
        <label class="check"><input v-model="termos" type="checkbox" /> Entendo que o Sentinel Trade é um sistema simulado e aceito os termos de uso.</label>
        <div v-if="erro" class="alert err">{{ erro }}</div>
        <button class="btn" :disabled="!valido || carregando">{{ carregando ? 'Criando…' : 'Criar conta' }}</button>
        <div style="text-align:center;margin-top:.8rem"><RouterLink to="/login" class="link">Já tenho conta</RouterLink></div>
      </form>

      <form v-else-if="etapa === 2" class="auth-card" @submit.prevent="confirmar">
        <p style="margin:0">Configure a verificação em duas etapas</p>
        <p class="small" style="opacity:.8">Escaneie o QR code no seu aplicativo autenticador (Google Authenticator, Authy…) ou digite a chave manualmente.</p>
        <img v-if="qr" :src="qr" class="qr" width="180" height="180" alt="QR code do autenticador" />
        <div class="secret">{{ mfa.segredo }}</div>
        <label for="cod">Código de 6 dígitos:</label>
        <input id="cod" v-model="codigo" class="input code" inputmode="numeric" maxlength="6" autocomplete="one-time-code" required />
        <div v-if="erro" class="alert err">{{ erro }}</div>
        <button class="btn" :disabled="codigo.length !== 6 || carregando">{{ carregando ? 'Confirmando…' : 'Confirmar e finalizar' }}</button>
        <p class="demo">Demonstração: qualquer código de 6 dígitos, exceto 000000.</p>
      </form>

      <div v-else class="auth-card">
        <p style="margin:0">Conta criada!</p>
        <div class="alert ok">Tudo pronto, {{ f.nome.split(' ')[0] }}.<span v-if="saldo"> Sua conta simulada recebeu {{ brl(saldo) }} de saldo inicial.</span></div>
        <button class="btn" @click="router.push('/login')">Ir para o login</button>
      </div>
    </div>
  </div>
</template>
