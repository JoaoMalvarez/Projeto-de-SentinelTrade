import { createRouter, createWebHistory } from 'vue-router'
import { session } from './store/session'

const v = (n) => () => import(`./views/${n}.vue`)

const routes = [
  { path: '/', name: 'home', component: v('Landing'), meta: { public: true } },
  { path: '/login', name: 'login', component: v('Login'), meta: { public: true, bare: true } },
  { path: '/cadastro', component: v('Cadastro'), meta: { public: true, bare: true } },
  { path: '/mfa', name: 'mfa', component: v('Mfa'), meta: { bare: true } },
  { path: '/esqueci-senha', component: v('EsqueciSenha'), meta: { public: true, bare: true } },
  { path: '/dashboard', component: v('Dashboard'), meta: { roles: ['INVESTIDOR'] } },
  { path: '/cotacoes', component: v('Cotacoes'), meta: { roles: ['INVESTIDOR'] } },
  { path: '/ativos/:ticker', component: v('Ativo'), meta: { roles: ['INVESTIDOR'] } },
  { path: '/ordens', component: v('Ordens'), meta: { roles: ['INVESTIDOR'] } },
  { path: '/ordens/:id', component: v('OrdemDetalhe'), meta: { roles: ['INVESTIDOR'] } },
  { path: '/carteira', component: v('Carteira'), meta: { roles: ['INVESTIDOR'] } },
  { path: '/notificacoes', component: v('Notificacoes'), meta: { roles: ['INVESTIDOR'] } },
  { path: '/auditoria', component: v('Auditoria'), meta: { roles: ['AUDITOR', 'ADMIN'] } },
  { path: '/admin', component: v('Admin'), meta: { roles: ['ADMIN'] } },
  { path: '/indisponivel', component: v('StatusPage'), props: { icone: '⚠', titulo: 'Sistema em modo seguro', texto: 'Estamos com indisponibilidade controlada para proteger suas operações. Suas ordens já enviadas continuam sendo processadas e nada foi perdido. Tente novamente em instantes.', acao: 'Tentar novamente', destino: 'inicio' }, meta: { public: true } },
  { path: '/403', component: v('StatusPage'), props: { icone: '🔒', titulo: 'Sem permissão', texto: 'Seu perfil não tem acesso a esta área.', acao: 'Voltar ao início', destino: 'inicio' }, meta: { public: true } },
  { path: '/:rest(.*)*', component: v('StatusPage'), props: { icone: '404', titulo: 'Página não encontrada', texto: 'O endereço que você acessou não existe.', acao: 'Ir para o início', destino: '/' }, meta: { public: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: (to) => (to.hash ? { el: to.hash, behavior: 'smooth' } : { top: 0 })
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (to.name === 'mfa') return session.mfaToken ? true : '/login'
  if (!session.token) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.roles && !to.meta.roles.includes(session.user?.perfil)) return '/403'
  return true
})

export default router
