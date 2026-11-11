// Dados simulados alinhados com database/seed.sql. Simula as regras do backend.
import { session } from '../store/session'
import { brl, FINAIS, CANCELAVEIS } from '../utils'

const sleep = (ms = 250) => new Promise((r) => setTimeout(r, ms))
const fail = (status, message) => { throw { status, message } }
const now = () => new Date().toISOString()
const ago = (min) => new Date(Date.now() - min * 60000).toISOString()
const r2 = (n) => Math.round(n * 100) / 100
const SENHA = 'Teste@123'

const USERS = {
  'ana@orion.test': { id: 'u1', nome: 'Ana Investidora', email: 'ana@orion.test', perfil: 'INVESTIDOR' },
  'bruno@orion.test': { id: 'u2', nome: 'Bruno Trader', email: 'bruno@orion.test', perfil: 'INVESTIDOR' },
  'auditor@orion.test': { id: 'u3', nome: 'Carla Auditora', email: 'auditor@orion.test', perfil: 'AUDITOR' },
  'admin@orion.test': { id: 'u4', nome: 'Diego Admin', email: 'admin@orion.test', perfil: 'ADMIN' }
}
const byId = (id) => Object.values(USERS).find((u) => u.id === id)
const me = () => byId((session.token || '').replace('mock-', '')) || fail(401, 'Sessão expirada.')
const exigir = (...perfis) => { const u = me(); if (!perfis.includes(u.perfil)) fail(403, 'Sem permissão.'); return u }

const conta = { u1: { disp: 5000, bloq: 0 }, u2: { disp: 250000, bloq: 0 } }
const limites = {
  u1: { porOrdem: 20000, diario: 50000, ordensDia: 20 },
  u2: { porOrdem: 200000, diario: 500000, ordensDia: 100 }
}
const posicoes = {
  u1: [{ ticker: 'PETR4', qtd: 100, pm: 35.2 }],
  u2: [{ ticker: 'VALE3', qtd: 500, pm: 58 }, { ticker: 'BOVA11', qtd: 300, pm: 120 }, { ticker: 'HGLG11', qtd: 200, pm: 155 }]
}
const ativosDb = [
  ['PETR4', 'Petrobras PN', 'ACAO', 38.5, 38.1, 'ABERTO'],
  ['VALE3', 'Vale ON', 'ACAO', 62.1, 61.4, 'ABERTO'],
  ['ITUB4', 'Itaú Unibanco PN', 'ACAO', 33.75, 33.9, 'ABERTO'],
  ['MGLU3', 'Magazine Luiza ON', 'ACAO', 9.8, 9.8, 'FECHADO'],
  ['BOVA11', 'ETF iShares Ibovespa', 'ETF', 128.4, 127.2, 'ABERTO'],
  ['IVVB11', 'ETF iShares S&P 500', 'ETF', 305.2, 306.5, 'ABERTO'],
  ['HGLG11', 'CSHG Logística FII', 'FII', 160.9, 160.1, 'ABERTO'],
  ['MXRF11', 'Maxi Renda FII', 'FII', 10.35, 10.3, 'ABERTO']
].map(([ticker, nome, tipo, preco, abertura, mercado]) => ({ ticker, nome, tipo, preco, abertura, mercado }))
const viewAtivo = (a) => ({ ...a, variacao: r2((a.preco / a.abertura - 1) * 100), atualizadoEm: now() })
const achar = (t) => ativosDb.find((a) => a.ticker === t)
let modoSeguro = false

// ---------- auditoria (cadeia de hashes, simula o trigger do banco) ----------
const hash = (s) => { let h = 0x811c9dc5; for (const c of s) { h ^= c.charCodeAt(0); h = Math.imul(h, 0x01000193) >>> 0 } return h.toString(16).padStart(8, '0').repeat(8) }
const calc = (prev, l) => hash(`${prev || ''}|${l.ocorridoEm}|${l.usuario}|${l.evento}|${l.entidade}|${l.entidadeId}`)
const logs = []
function audit(u, evento, entidade, entidadeId, ocorridoEm = now()) {
  const l = { id: logs.length + 1, ocorridoEm, usuario: u?.email || '—', evento, entidade, entidadeId: String(entidadeId || '') }
  l.hashAnterior = logs.at(-1)?.hashRegistro || null
  l.hashRegistro = calc(l.hashAnterior, l)
  logs.push(l)
}

// ---------- ordens ----------
const ev = (status, em, detalhe = '') => ({ status, em, detalhe })
let seq = 3
const idem = {}
const ordensDb = [
  { id: 'ORD-0001', userId: 'u2', ticker: 'VALE3', tipo: 'COMPRA', quantidade: 100, precoLimite: 62.5, status: 'EXECUTADA', quantidadeExecutada: 100, precoExecucao: 62.1, motivoRejeicao: null, criadoEm: ago(120),
    eventos: [ev('PENDENTE', ago(120)), ev('VALIDADA', ago(119)), ev('ENVIADA_BOLSA', ago(119)), ev('EXECUTADA', ago(118), 'Execução a R$ 62,10')] },
  { id: 'ORD-0002', userId: 'u1', ticker: 'PETR4', tipo: 'COMPRA', quantidade: 100, precoLimite: 35.5, status: 'EXECUTADA', quantidadeExecutada: 100, precoExecucao: 35.2, motivoRejeicao: null, criadoEm: ago(4300),
    eventos: [ev('PENDENTE', ago(4300)), ev('VALIDADA', ago(4299)), ev('ENVIADA_BOLSA', ago(4299)), ev('EXECUTADA', ago(4298), 'Execução a R$ 35,20')] },
  { id: 'ORD-0003', userId: 'u1', ticker: 'MGLU3', tipo: 'COMPRA', quantidade: 50, precoLimite: 9.9, status: 'REJEITADA', quantidadeExecutada: 0, precoExecucao: null, motivoRejeicao: 'Mercado fechado para MGLU3.', criadoEm: ago(1500),
    eventos: [ev('PENDENTE', ago(1500)), ev('REJEITADA', ago(1500), 'Mercado fechado para MGLU3.')] }
]
const pubOrdem = (o) => { const { userId, ...r } = o; return { ...r, eventos: [...o.eventos] } }
const minhas = (u) => ordensDb.filter((o) => o.userId === u.id)

// ---------- notificações ----------
const notifs = [
  { id: 1, userId: 'u2', ordemId: 'ORD-0001', mensagem: 'Sua ordem de compra de 100 VALE3 foi executada a R$ 62,10.', lida: false, criadaEm: ago(118) },
  { id: 2, userId: 'u1', ordemId: 'ORD-0003', mensagem: 'Sua ordem de compra de 50 MGLU3 foi rejeitada: Mercado fechado para MGLU3.', lida: true, criadaEm: ago(1500) }
]
function notificar(o, mensagem) { notifs.unshift({ id: notifs.length + 1, userId: o.userId, ordemId: o.id, mensagem, lida: false, criadaEm: now() }) }

audit(USERS['ana@orion.test'], 'LOGIN_OK', 'INVESTIDOR', 'u1', ago(4310))
audit(USERS['ana@orion.test'], 'ORDEM_CRIADA', 'ORDEM', 'ORD-0002', ago(4300))
audit(USERS['ana@orion.test'], 'ORDEM_REJEITADA', 'ORDEM', 'ORD-0003', ago(1500))

function mudar(o, status, detalhe) {
  o.status = status; o.eventos.push(ev(status, now(), detalhe))
  audit(byId(o.userId), `ORDEM_${status}`, 'ORDEM', o.id)
}
function liberar(o) { // devolve o saldo ainda reservado de uma compra
  if (o.tipo !== 'COMPRA') return
  const c = conta[o.userId], resto = r2((o.quantidade - o.quantidadeExecutada) * o.precoLimite)
  c.bloq = r2(c.bloq - resto); c.disp = r2(c.disp + resto)
}
function liquidar(o, q, preco) {
  const c = conta[o.userId], pos = posicoes[o.userId]
  if (o.tipo === 'COMPRA') {
    c.bloq = r2(c.bloq - q * o.precoLimite); c.disp = r2(c.disp + q * (o.precoLimite - preco))
    let p = pos.find((x) => x.ticker === o.ticker)
    if (!p) { p = { ticker: o.ticker, qtd: 0, pm: 0 }; pos.push(p) }
    p.pm = r2((p.pm * p.qtd + preco * q) / (p.qtd + q)); p.qtd += q
  } else {
    const p = pos.find((x) => x.ticker === o.ticker)
    p.qtd -= q; c.disp = r2(c.disp + q * preco)
    if (p.qtd === 0) pos.splice(pos.indexOf(p), 1)
  }
  o.quantidadeExecutada += q
}
function executar(o) {
  if (FINAIS.includes(o.status)) return
  const a = achar(o.ticker)
  const preco = o.tipo === 'COMPRA' ? Math.min(a.preco, o.precoLimite) : Math.max(a.preco, o.precoLimite)
  const parcial = o.quantidade >= 500 && o.quantidadeExecutada === 0
  const q = parcial ? Math.floor(o.quantidade * 0.6) : o.quantidade - o.quantidadeExecutada
  liquidar(o, q, preco); o.precoExecucao = preco
  if (parcial) { mudar(o, 'PARCIALMENTE_EXECUTADA', `${q} de ${o.quantidade} executadas a ${brl(preco)}`); setTimeout(() => executar(o), 2500) }
  else { mudar(o, 'EXECUTADA', `Execução a ${brl(preco)}`); notificar(o, `Sua ordem de ${o.tipo === 'COMPRA' ? 'compra' : 'venda'} de ${o.quantidade} ${o.ticker} foi executada a ${brl(preco)}.`) }
}
function agendar(o) {
  setTimeout(() => !FINAIS.includes(o.status) && mudar(o, 'VALIDADA'), 900)
  setTimeout(() => !FINAIS.includes(o.status) && mudar(o, 'ENVIADA_BOLSA', 'Transmitida à bolsa simulada'), 2200)
  setTimeout(() => executar(o), 4200)
}
const hoje = (iso) => new Date(iso).toDateString() === new Date().toDateString()

function validar(u, o) {
  const a = achar(o.ticker)
  if (!a) return 'Ativo inexistente.'
  if (a.mercado !== 'ABERTO') return `Mercado fechado para ${a.ticker}.`
  const total = o.quantidade * o.precoLimite, l = limites[u.id], c = conta[u.id]
  if (total > l.porOrdem) return `Limite de risco excedido: máximo de ${brl(l.porOrdem)} por ordem.`
  const dia = minhas(u).filter((x) => x.id !== o.id && !['REJEITADA', 'CANCELADA'].includes(x.status) && hoje(x.criadoEm))
  if (dia.reduce((s, x) => s + x.quantidade * x.precoLimite, 0) + total > l.diario) return `Limite diário excedido: máximo de ${brl(l.diario)}.`
  if (dia.length >= l.ordensDia) return 'Quantidade máxima de ordens diárias atingida.'
  if (o.tipo === 'COMPRA' && total > c.disp) return `Saldo insuficiente: necessário ${brl(total)}, disponível ${brl(c.disp)}.`
  if (o.tipo === 'VENDA') {
    const tem = posicoes[u.id].find((p) => p.ticker === o.ticker)?.qtd || 0
    const res = minhas(u).filter((x) => x.id !== o.id && x.tipo === 'VENDA' && x.ticker === o.ticker && !FINAIS.includes(x.status)).reduce((s, x) => s + x.quantidade - x.quantidadeExecutada, 0)
    if (o.quantidade > tem - res) return `Posição insuficiente: você possui ${tem - res} ${o.ticker} disponíveis.`
  }
  return null
}

// ================= API =================
export async function login(email, senha) {
  await sleep()
  const u = USERS[(email || '').toLowerCase()]
  if (!u || senha !== (u.senha || SENHA)) { audit(null, 'LOGIN_FALHA', 'INVESTIDOR', email); fail(401, 'Credenciais inválidas.') }
  return { mfaToken: `mfa-${u.id}` }
}
export async function verificarMfa(mfaToken, codigo) {
  await sleep()
  const u = byId(String(mfaToken).replace('mfa-', ''))
  if (!u || !/^\d{6}$/.test(codigo) || codigo === '000000') { audit(u, 'MFA_FALHA', 'INVESTIDOR', u?.id); fail(401, 'Código inválido ou expirado.') }
  audit(u, 'LOGIN_OK', 'INVESTIDOR', u.id)
  const { senha: _omitir, ...usuario } = u // nunca devolver a senha
  return { token: `mock-${u.id}`, usuario }
}
// ---------- cadastro de usuário (RF01) ----------
const pendentes = {}
const SENHA_FORTE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,}$/
const B32 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
export async function cadastrar({ nome, email, senha }) {
  await sleep(400)
  nome = (nome || '').trim(); email = (email || '').trim().toLowerCase()
  if (nome.length < 3) fail(400, 'Informe seu nome completo.')
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) fail(400, 'E-mail inválido.')
  if (!SENHA_FORTE.test(senha || '')) fail(400, 'A senha deve ter 8+ caracteres, com maiúscula, minúscula, número e símbolo.')
  if (USERS[email]) fail(409, 'Este e-mail já está cadastrado.')
  Object.keys(pendentes).forEach((k) => pendentes[k].email === email && delete pendentes[k])
  const segredo = Array.from({ length: 16 }, () => B32[Math.floor(Math.random() * 32)]).join('')
  const cadastroToken = `cad-${Math.random().toString(36).slice(2)}${Date.now()}`
  pendentes[cadastroToken] = { nome, email, senha, segredo }
  return { cadastroToken, mfa: { segredo, otpauthUri: `otpauth://totp/SentinelTrade:${encodeURIComponent(email)}?secret=${segredo}&issuer=SentinelTrade` } }
}
export async function confirmarMfaCadastro(cadastroToken, codigo) {
  await sleep(400)
  const p = pendentes[cadastroToken] || fail(400, 'Cadastro expirado. Recomece.')
  if (!/^\d{6}$/.test(codigo) || codigo === '000000') fail(401, 'Código inválido ou expirado.')
  const id = `u${Object.keys(USERS).length + 1}`
  USERS[p.email] = { id, nome: p.nome, email: p.email, perfil: 'INVESTIDOR', senha: p.senha } // mock: o backend real guarda só o hash
  conta[id] = { disp: 10000, bloq: 0 } // saldo inicial simulado
  limites[id] = { porOrdem: 20000, diario: 50000, ordensDia: 20 }
  posicoes[id] = []
  delete pendentes[cadastroToken]
  audit(USERS[p.email], 'USUARIO_CRIADO', 'INVESTIDOR', id)
  return { mensagem: 'Conta criada com sucesso.', saldoInicial: 10000 }
}
export async function esqueciSenha() { await sleep(); return { mensagem: 'Se o e-mail estiver cadastrado, você receberá as instruções.' } }
export async function logout() { await sleep(100); return null }

export async function dashboard() {
  await sleep(150)
  const u = exigir('INVESTIDOR'), c = conta[u.id]
  const valor = posicoes[u.id].reduce((s, p) => s + p.qtd * achar(p.ticker).preco, 0)
  return {
    saldoDisponivel: c.disp, saldoBloqueado: c.bloq, patrimonio: r2(c.disp + c.bloq + valor),
    ordensRecentes: minhas(u).sort((a, b) => b.criadoEm.localeCompare(a.criadoEm)).slice(0, 5).map(pubOrdem),
    destaques: ativosDb.filter((a) => a.mercado === 'ABERTO').map(viewAtivo).sort((a, b) => Math.abs(b.variacao) - Math.abs(a.variacao)).slice(0, 4)
  }
}
export async function carteira() {
  await sleep(150)
  const u = exigir('INVESTIDOR'), c = conta[u.id]
  return {
    saldoDisponivel: c.disp, saldoBloqueado: c.bloq, limites: limites[u.id],
    posicoes: posicoes[u.id].map((p) => {
      const a = achar(p.ticker), valorAtual = r2(p.qtd * a.preco), custo = p.qtd * p.pm
      return { ticker: p.ticker, nome: a.nome, quantidade: p.qtd, precoMedio: p.pm, precoAtual: a.preco, valorAtual, resultado: r2(valorAtual - custo), resultadoPct: r2((valorAtual / custo - 1) * 100) }
    })
  }
}

export async function ativos() { await sleep(150); me(); return ativosDb.map(viewAtivo) }
export async function ativo(ticker) {
  await sleep(150)
  const u = me(), a = achar(ticker) || fail(404, 'Ativo não encontrado.')
  let s = [...a.ticker].reduce((x, c) => x + c.charCodeAt(0), 7), p = a.abertura
  const serie = Array.from({ length: 30 }, () => { s = (s * 9301 + 49297) % 233280; p *= 1 + (s / 233280 - 0.5) * 0.03; return r2(p) })
  serie[29] = a.preco
  const pos = (posicoes[u.id] || []).find((x) => x.ticker === ticker)
  return { ...viewAtivo(a), serie, posicao: pos ? { quantidade: pos.qtd, precoMedio: pos.pm } : null }
}
export function subscribeCotacoes(cb) {
  const t = setInterval(() => {
    ativosDb.forEach((a) => { if (a.mercado === 'ABERTO') a.preco = Math.max(0.5, r2(a.preco * (1 + (Math.random() - 0.5) * 0.006))) })
    cb(ativosDb.map(viewAtivo))
  }, 3000)
  return () => clearInterval(t)
}

export async function criarOrdem(dados, chave) {
  await sleep(400)
  const u = exigir('INVESTIDOR')
  if (modoSeguro) fail(503, 'Sistema em modo seguro: novas ordens indisponíveis.')
  if (!chave) fail(400, 'Idempotency-Key obrigatória.')
  if (idem[chave]) return pubOrdem(idem[chave])
  const q = Number(dados.quantidade), p = Number(dados.precoLimite)
  if (!Number.isInteger(q) || q <= 0 || !(p > 0) || !['COMPRA', 'VENDA'].includes(dados.tipo)) fail(400, 'Dados da ordem inválidos.')
  const o = { id: `ORD-${String(++seq).padStart(4, '0')}`, userId: u.id, ticker: dados.ticker, tipo: dados.tipo, quantidade: q, precoLimite: p, status: 'PENDENTE', quantidadeExecutada: 0, precoExecucao: null, motivoRejeicao: null, criadoEm: now(), eventos: [ev('PENDENTE', now())] }
  idem[chave] = o; ordensDb.push(o)
  audit(u, 'ORDEM_CRIADA', 'ORDEM', o.id)
  const motivo = validar(u, o)
  if (motivo) {
    o.motivoRejeicao = motivo; mudar(o, 'REJEITADA', motivo)
    notificar(o, `Sua ordem de ${o.tipo === 'COMPRA' ? 'compra' : 'venda'} de ${o.quantidade} ${o.ticker} foi rejeitada: ${motivo}`)
  } else {
    if (o.tipo === 'COMPRA') { const t = r2(q * p); conta[u.id].disp = r2(conta[u.id].disp - t); conta[u.id].bloq = r2(conta[u.id].bloq + t) }
    agendar(o)
  }
  return pubOrdem(o)
}
export async function ordens(f = {}) {
  await sleep(120)
  const u = exigir('INVESTIDOR')
  return minhas(u)
    .filter((o) => (!f.status || o.status === f.status) && (!f.tipo || o.tipo === f.tipo) &&
      (f.aba === 'andamento' ? !FINAIS.includes(o.status) : f.aba === 'historico' ? FINAIS.includes(o.status) : true))
    .sort((a, b) => b.criadoEm.localeCompare(a.criadoEm)).map(pubOrdem)
}
export async function ordem(id) {
  await sleep(100)
  const u = exigir('INVESTIDOR'), o = minhas(u).find((x) => x.id === id)
  return o ? pubOrdem(o) : fail(404, 'Ordem não encontrada.')
}
export async function cancelarOrdem(id) {
  await sleep(300)
  const u = exigir('INVESTIDOR'), o = minhas(u).find((x) => x.id === id) || fail(404, 'Ordem não encontrada.')
  if (!CANCELAVEIS.includes(o.status)) fail(409, 'Esta ordem não pode mais ser cancelada.')
  liberar(o); mudar(o, 'CANCELADA', 'Cancelada pelo investidor')
  notificar(o, `Sua ordem ${o.id} (${o.ticker}) foi cancelada.`)
  return pubOrdem(o)
}

export async function notificacoes() { await sleep(100); const u = me(); return notifs.filter((n) => n.userId === u.id) }
export async function marcarLida(id) { await sleep(100); const n = notifs.find((x) => x.id === id); if (n) n.lida = true; return null }

export async function auditoria(f = {}) {
  await sleep(150); exigir('AUDITOR', 'ADMIN')
  const q = (f.q || '').toLowerCase()
  return [...logs].reverse().filter((l) => (!f.evento || l.evento === f.evento) && (!q || `${l.usuario} ${l.entidadeId}`.toLowerCase().includes(q)))
}
export async function verificarIntegridade() {
  await sleep(500); exigir('AUDITOR', 'ADMIN')
  let prev = null
  for (const l of logs) {
    if (l.hashAnterior !== prev || l.hashRegistro !== calc(prev, l)) return { ok: false, verificados: l.id - 1, falhaEm: l.id }
    prev = l.hashRegistro
  }
  return { ok: true, verificados: logs.length }
}

export async function adminEstado() {
  await sleep(150); exigir('ADMIN')
  return {
    modoSeguro, ativos: ativosDb.map(viewAtivo),
    investidores: Object.values(USERS).filter((u) => u.perfil === 'INVESTIDOR').map((u) => ({ id: u.id, nome: u.nome, email: u.email, limites: { ...limites[u.id] } }))
  }
}
export async function setLimites(id, l) {
  await sleep(250); const u = exigir('ADMIN')
  if (![l.porOrdem, l.diario, l.ordensDia].every((v) => Number(v) > 0)) fail(400, 'Limites devem ser positivos.')
  limites[id] = { porOrdem: Number(l.porOrdem), diario: Number(l.diario), ordensDia: Number(l.ordensDia) }
  audit(u, 'LIMITES_ALTERADOS', 'LIMITE', id); return null
}
export async function setMercado(ticker, status) {
  await sleep(200); const u = exigir('ADMIN'); achar(ticker).mercado = status
  audit(u, 'MERCADO_ALTERADO', 'ATIVO', `${ticker}:${status}`); return null
}
export async function setModoSeguro(ativo) {
  await sleep(200); const u = exigir('ADMIN'); modoSeguro = !!ativo
  audit(u, ativo ? 'MODO_SEGURO_ON' : 'MODO_SEGURO_OFF', 'SISTEMA', ''); return null
}
