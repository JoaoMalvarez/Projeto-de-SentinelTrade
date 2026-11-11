<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { session, clearSession } from '../store/session'
import { api } from '../services/api'
import { norm, brl } from '../utils'

const router = useRouter()
const q = ref('')
const naoLidas = ref(0)
const ativos = ref([])
const aberto = ref(false)
const destaque = ref(-1)
const rolou = ref(false)
const logado = computed(() => !!session.token)
const perfil = computed(() => session.user?.perfil)
const investidor = computed(() => logado.value && perfil.value === 'INVESTIDOR')
let t

const aoRolar = () => { rolou.value = window.scrollY > 8 }
async function carregar() {
  if (!investidor.value) return (naoLidas.value = 0)
  try { naoLidas.value = (await api.notificacoes()).filter((n) => !n.lida).length } catch {}
}
onMounted(() => { carregar(); t = setInterval(carregar, 4000); window.addEventListener('scroll', aoRolar, { passive: true }); aoRolar() })
onUnmounted(() => { clearInterval(t); window.removeEventListener('scroll', aoRolar) })

// ---------- pesquisa ----------
// Investidor: sugestões de ativos enquanto digita (ignora acento e maiúscula).
// Auditor/Admin: busca nos logs. Visitante: pede login.
const sugestoes = computed(() => {
  const termo = norm(q.value.trim())
  if (!termo || !investidor.value) return []
  return ativos.value.filter((a) => norm(`${a.ticker} ${a.nome}`).includes(termo)).slice(0, 6)
})
async function aoFocar() {
  aberto.value = true
  if (investidor.value) { try { ativos.value = await api.ativos() } catch {} }
}
function aoDigitar() { aberto.value = true; destaque.value = -1 }
function mover(d) {
  const n = sugestoes.value.length
  if (!n) return
  destaque.value = destaque.value === -1 ? (d > 0 ? 0 : n - 1) : (destaque.value + d + n) % n
}
function abrirAtivo(a) {
  aberto.value = false; q.value = ''; destaque.value = -1
  router.push(`/ativos/${a.ticker}`)
}
function buscar() {
  const texto = q.value.trim()
  if (!logado.value) return router.push('/login')
  if (!investidor.value) return router.push({ path: '/auditoria', query: texto ? { q: texto } : {} })
  if (destaque.value >= 0 && sugestoes.value[destaque.value]) return abrirAtivo(sugestoes.value[destaque.value])
  aberto.value = false
  router.push({ path: '/cotacoes', query: texto ? { q: texto } : {} })
}
function verTodos() { destaque.value = -1; buscar() }

async function sair() {
  try { await api.logout() } catch {}
  clearSession(); router.push('/')
}
</script>

<template>
  <header class="nav" :class="{ rolou }">
    <RouterLink v-if="logado" to="/" class="nav-logo">Sentinel Trade</RouterLink>

    <form class="nav-search" role="search" @submit.prevent="buscar">
        <img src="/search.png" alt="" class="search-icon" />
      <input
        v-model="q" class="input" placeholder="Pesquisar" aria-label="Pesquisar" autocomplete="off"
        @focus="aoFocar" @input="aoDigitar" @blur="aberto = false"
        @keydown.down.prevent="mover(1)" @keydown.up.prevent="mover(-1)" @keydown.esc="aberto = false"
      />
      <ul v-if="aberto && q.trim() && investidor" class="sugestoes">
        <li v-for="(a, i) in sugestoes" :key="a.ticker" :class="{ on: i === destaque }" @mousedown.prevent="abrirAtivo(a)" @mousemove="destaque = i">
          <strong>{{ a.ticker }}</strong><span class="muted">{{ a.nome }}</span><span class="preco">{{ brl(a.preco) }}</span>
        </li>
        <li v-if="!sugestoes.length" class="muted">Nenhum ativo encontrado</li>
        <li v-else class="todos" @mousedown.prevent="verTodos">Ver todos os resultados</li>
      </ul>
    </form>

    <nav v-if="!logado" class="nav-links">
      <RouterLink :to="{ path: '/', hash: '#produtos' }">Produtos</RouterLink>
      <RouterLink :to="{ path: '/', hash: '#mercados' }">Mercados</RouterLink>
      <RouterLink :to="{ path: '/', hash: '#corretoras' }">Corretoras</RouterLink>
      <RouterLink :to="{ path: '/', hash: '#mais' }">Mais</RouterLink>
    </nav>
    <nav v-else class="nav-links">
      <template v-if="perfil === 'INVESTIDOR'">
        <RouterLink to="/dashboard">Dashboard</RouterLink>
        <RouterLink to="/cotacoes">Cotações</RouterLink>
        <RouterLink to="/ordens">Ordens</RouterLink>
        <RouterLink to="/carteira">Carteira</RouterLink>
        <RouterLink to="/notificacoes">Notificações<span v-if="naoLidas" class="dot">{{ naoLidas }}</span></RouterLink>
      </template>
      <RouterLink v-if="perfil === 'AUDITOR' || perfil === 'ADMIN'" to="/auditoria">Auditoria</RouterLink>
      <RouterLink v-if="perfil === 'ADMIN'" to="/admin">Administração</RouterLink>
    </nav>

    <RouterLink v-if="!logado" to="/login" class="btn">Entrar</RouterLink>
    <div v-else class="nav-user">
      <span class="muted small">{{ session.user?.nome }}</span>
      <button class="btn" @click="sair">Sair</button>
    </div>
  </header>
</template>
