# Frontend - SentinelTrade (Vue 3 + Vite)

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

## Modo demonstração (padrão)
Com `VITE_USE_MOCK=true` (padrão) nada depende de backend: os dados vêm de `src/mocks/mockApi.js`,
que simula as regras (saldo, posição, limite, mercado fechado, idempotência, ciclo de vida da ordem).

Contas de teste (mesmas do `database/seed.sql`), senha `Teste@123`, código MFA: qualquer 6 dígitos (exceto `000000`):

| E-mail | Perfil |
|---|---|
| ana@orion.test | Investidor (saldo R$ 5.000, 100 PETR4) |
| bruno@orion.test | Investidor |
| auditor@orion.test | Auditor |
| admin@orion.test | Admin |

Também dá para criar uma conta nova em `/cadastro` (nome, e-mail, senha forte e configuração do MFA com QR code). A conta nova começa com R$ 10.000 simulados.

## Ligando ao BFF real
1. Copie `.env.example` para `.env` e coloque `VITE_USE_MOCK=false`.
2. Suba o BFF em `VITE_BFF_URL`. O contrato esperado está em `docs/backend-e-bff.md`.
3. Nenhuma tela muda: todas chamam apenas `src/services/api.js`.

## Estrutura
```
src/
├── views/        # uma tela por arquivo
├── components/   # NavBar, StatusBadge, OrdemModal
├── services/     # api.js (único ponto de acesso ao BFF), http.js
├── mocks/        # mockApi.js (dados simulados)
├── store/        # session.js
├── router.js     # rotas e guardas por perfil
└── styles.css    # design tokens
```
