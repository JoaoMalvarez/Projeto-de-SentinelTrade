<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../services/api'
import { brl, pct, cor, norm, TIPOS } from '../utils'

const route = useRoute()
const lista = ref([]), erro = ref(''), carregando = ref(true)
const busca = ref(route.query.q || ''), tipo = ref('')
const atualizado = ref(Date.now()), agora = ref(Date.now())
let parar, tick
// pesquisar de novo pelo menu, já estando nesta tela, atualiza o filtro
watch(() => route.query.q, (v) => { busca.value = v || '' })

onMounted(async () => {
  try { lista.value = await api.ativos(); atualizado.value = Date.now() } catch (e) { erro.value = e.message } finally { carregando.value = false }
  parar = api.subscribeCotacoes((l) => { lista.value = l; atualizado.value = Date.now() })
  tick = setInterval(() => (agora.value = Date.now()), 1000)
})
onUnmounted(() => { parar?.(); clearInterval(tick) })

const filtrada = computed(() => lista.value.filter((a) =>
  (!tipo.value || a.tipo === tipo.value) &&
  (!busca.value || norm(`${a.ticker} ${a.nome}`).includes(norm(busca.value)))))
const segundos = computed(() => Math.max(0, Math.floor((agora.value - atualizado.value) / 1000)))
</script>

<template>
  <main class="page">
    <div class="row between"><h1>Cotações</h1><span class="muted small">Atualizado há {{ segundos }}s</span></div>
    <div class="filters">
      <input v-model="busca" class="input" placeholder="Buscar ticker ou nome" />
      <select v-model="tipo" class="input"><option value="">Todos os tipos</option><option v-for="(n, k) in TIPOS" :key="k" :value="k">{{ n }}</option></select>
    </div>
    <p v-if="erro" class="down">{{ erro }}</p>
    <p v-else-if="carregando" class="muted">Carregando…</p>
    <div v-else class="card tablewrap"><table>
      <thead><tr><th>Ativo</th><th>Nome</th><th>Tipo</th><th class="r">Preço</th><th class="r">Variação</th><th>Mercado</th></tr></thead>
      <tbody>
        <tr v-for="a in filtrada" :key="a.ticker" class="click" @click="$router.push(`/ativos/${a.ticker}`)">
          <td><strong>{{ a.ticker }}</strong></td><td class="muted">{{ a.nome }}</td><td>{{ TIPOS[a.tipo] }}</td>
          <td class="r">{{ brl(a.preco) }}</td><td class="r" :class="cor(a.variacao)">{{ pct(a.variacao) }}</td>
          <td><span class="badge" :class="a.mercado === 'ABERTO' ? 'up' : 'muted'">{{ a.mercado === 'ABERTO' ? 'Aberto' : 'Fechado' }}</span></td>
        </tr>
        <tr v-if="!filtrada.length"><td colspan="6" class="muted">Nenhum ativo encontrado.</td></tr>
      </tbody>
    </table></div>
  </main>
</template>
