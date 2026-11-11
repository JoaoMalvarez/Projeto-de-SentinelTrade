// ÚNICO ponto de acesso a dados do frontend.
// Modo mock (padrão): usa src/mocks/mockApi.js. Modo real: chama o BFF (contrato em docs/backend-e-bff.md).
import { http, BASE } from './http'
import * as mock from '../mocks/mockApi'
import { session } from '../store/session'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

// Preenchidos em main.js (evita dependência circular com o router)
export const hooks = { unauthorized() {}, forbidden() {}, unavailable() {} }

async function run(fn) {
  try { return await fn() } catch (e) {
    if (e?.status === 401 && session.token) hooks.unauthorized()
    else if (e?.status === 403) hooks.forbidden()
    else if (e?.status === 503) hooks.unavailable()
    throw e
  }
}
const call = (real, fake) => run(() => (USE_MOCK ? fake() : real()))
const qs = (o = {}) => {
  const p = new URLSearchParams(Object.entries(o).filter(([, v]) => v)).toString()
  return p ? `?${p}` : ''
}

export const api = {
  // Autenticação (RF01)
  login: (email, senha) => call(() => http('POST', '/auth/login', { email, senha }), () => mock.login(email, senha)),
  verificarMfa: (mfaToken, codigo) => call(() => http('POST', '/auth/mfa', { mfaToken, codigo }), () => mock.verificarMfa(mfaToken, codigo)),
  cadastrar: (dados) => call(() => http('POST', '/auth/cadastro', dados), () => mock.cadastrar(dados)),
  confirmarMfaCadastro: (cadastroToken, codigo) => call(() => http('POST', '/auth/cadastro/mfa', { cadastroToken, codigo }), () => mock.confirmarMfaCadastro(cadastroToken, codigo)),
  esqueciSenha: (email) => call(() => http('POST', '/auth/esqueci-senha', { email }), () => mock.esqueciSenha(email)),
  logout: () => call(() => http('POST', '/auth/logout'), () => mock.logout()),

  // Visão geral, carteira e conta (RF02)
  dashboard: () => call(() => http('GET', '/dashboard'), () => mock.dashboard()),
  carteira: () => call(() => http('GET', '/carteira'), () => mock.carteira()),

  // Ativos e cotações (RF03)
  ativos: () => call(() => http('GET', '/ativos'), () => mock.ativos()),
  ativo: (ticker) => call(() => http('GET', `/ativos/${ticker}`), () => mock.ativo(ticker)),
  subscribeCotacoes(cb) {
    if (USE_MOCK) return mock.subscribeCotacoes(cb)
    const es = new EventSource(`${BASE}/api/cotacoes/stream`, { withCredentials: true })
    es.onmessage = (e) => cb(JSON.parse(e.data))
    return () => es.close()
  },

  // Ordens (RF04, RF05, RF06) - a Idempotency-Key evita ordens duplicadas (RNF02)
  criarOrdem: (ordem, chave) => call(() => http('POST', '/ordens', ordem, { 'Idempotency-Key': chave }), () => mock.criarOrdem(ordem, chave)),
  ordens: (f) => call(() => http('GET', `/ordens${qs(f)}`), () => mock.ordens(f)),
  ordem: (id) => call(() => http('GET', `/ordens/${id}`), () => mock.ordem(id)),
  cancelarOrdem: (id) => call(() => http('DELETE', `/ordens/${id}`), () => mock.cancelarOrdem(id)),

  // Notificações (RF07)
  notificacoes: () => call(() => http('GET', '/notificacoes'), () => mock.notificacoes()),
  marcarLida: (id) => call(() => http('PATCH', `/notificacoes/${id}/lida`), () => mock.marcarLida(id)),

  // Auditoria (RF08)
  auditoria: (f) => call(() => http('GET', `/auditoria${qs(f)}`), () => mock.auditoria(f)),
  verificarIntegridade: () => call(() => http('GET', '/auditoria/integridade'), () => mock.verificarIntegridade()),

  // Administração
  adminEstado: () => call(() => http('GET', '/admin/estado'), () => mock.adminEstado()),
  setLimites: (id, l) => call(() => http('PUT', `/admin/investidores/${id}/limites`, l), () => mock.setLimites(id, l)),
  setMercado: (ticker, status) => call(() => http('PUT', `/admin/ativos/${ticker}/mercado`, { status }), () => mock.setMercado(ticker, status)),
  setModoSeguro: (ativo) => call(() => http('PUT', '/admin/modo-seguro', { ativo }), () => mock.setModoSeguro(ativo))
}
