<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../services/api'
import { useLoad } from '../composables'
import { dt } from '../utils'

const route = useRoute()
const evento = ref(''), q = ref(route.query.q || ''), integ = ref(null), verificando = ref(false)
const EVENTOS = ['LOGIN_OK', 'LOGIN_FALHA', 'MFA_FALHA', 'ORDEM_CRIADA', 'ORDEM_VALIDADA', 'ORDEM_ENVIADA_BOLSA', 'ORDEM_EXECUTADA', 'ORDEM_REJEITADA', 'ORDEM_CANCELADA', 'USUARIO_CRIADO', 'LIMITES_ALTERADOS', 'MERCADO_ALTERADO']
const { data: logs, erro, carregando, recarregar } = useLoad(() => api.auditoria({ evento: evento.value, q: q.value }), 4000)
watch([evento, q], recarregar)
watch(() => route.query.q, (v) => { q.value = v || '' })

async function verificar() {
  verificando.value = true; integ.value = null
  try { integ.value = await api.verificarIntegridade() } finally { verificando.value = false }
}
const curto = (h) => (h ? `${h.slice(0, 10)}…` : '—')
</script>

<template>
  <main class="page">
    <div class="row between">
      <h1>Auditoria</h1>
      <button class="btn" :disabled="verificando" @click="verificar">{{ verificando ? 'Verificando…' : 'Verificar integridade' }}</button>
    </div>
    <div v-if="integ" class="card alert" :class="integ.ok ? 'ok' : 'err'">
      {{ integ.ok ? `Cadeia íntegra: ${integ.verificados} registros verificados.` : `Adulteração detectada no registro ${integ.falhaEm}.` }}
    </div>
    <p class="muted small">Registros somente leitura: não podem ser alterados nem apagados.</p>
    <div class="filters">
      <select v-model="evento" class="input"><option value="">Todos os eventos</option><option v-for="e in EVENTOS" :key="e">{{ e }}</option></select>
      <input v-model="q" class="input" placeholder="Usuário ou entidade" />
    </div>
    <p v-if="erro" class="down">{{ erro }}</p>
    <p v-else-if="carregando" class="muted">Carregando…</p>
    <div v-else class="card tablewrap"><table>
      <thead><tr><th>#</th><th>Quando</th><th>Usuário</th><th>Evento</th><th>Entidade</th><th>Hash anterior</th><th>Hash do registro</th></tr></thead>
      <tbody>
        <tr v-for="l in logs" :key="l.id">
          <td>{{ l.id }}</td><td class="muted">{{ dt(l.ocorridoEm) }}</td><td>{{ l.usuario }}</td><td>{{ l.evento }}</td>
          <td>{{ l.entidade }} {{ l.entidadeId }}</td><td class="muted small">{{ curto(l.hashAnterior) }}</td><td class="muted small">{{ curto(l.hashRegistro) }}</td>
        </tr>
        <tr v-if="!logs.length"><td colspan="7" class="muted">Nenhum registro.</td></tr>
      </tbody>
    </table></div>
  </main>
</template>
