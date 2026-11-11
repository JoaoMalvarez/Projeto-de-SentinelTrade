export const brl = (n) => Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
export const num = (n, d = 2) => Number(n).toLocaleString('pt-BR', { minimumFractionDigits: d, maximumFractionDigits: d })
export const pct = (n) => `${n > 0 ? '+' : ''}${num(n)}%`
export const dt = (iso) => new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
export const cor = (n) => (n > 0 ? 'up' : n < 0 ? 'down' : 'muted')

export const STATUS = {
  PENDENTE: ['Pendente', 'warn'],
  VALIDADA: ['Validada', 'info'],
  ENVIADA_BOLSA: ['Enviada à bolsa', 'info'],
  PARCIALMENTE_EXECUTADA: ['Parcialmente executada', 'warn'],
  EXECUTADA: ['Executada', 'up'],
  REJEITADA: ['Rejeitada', 'down'],
  CANCELADA: ['Cancelada', 'muted']
}
export const CANCELAVEIS = ['PENDENTE', 'VALIDADA', 'ENVIADA_BOLSA']
export const FINAIS = ['EXECUTADA', 'REJEITADA', 'CANCELADA']
export const TIMELINE = ['PENDENTE', 'VALIDADA', 'ENVIADA_BOLSA', 'EXECUTADA']
export const TIPOS = { ACAO: 'Ação', ETF: 'ETF', FII: 'FII' }
export const inicioDe = (perfil) => (perfil === 'AUDITOR' ? '/auditoria' : perfil === 'ADMIN' ? '/admin' : perfil === 'INVESTIDOR' ? '/dashboard' : '/')
export const norm = (s) => String(s).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
