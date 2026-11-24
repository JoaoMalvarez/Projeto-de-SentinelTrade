# SentinelTrade - atalhos de execucao
# Uso: make           (sobe tudo)   |   make help   (lista os comandos)

.DEFAULT_GOAL := run
.PHONY: run setup db-up db-down db-reset backend bff frontend install-frontend stop help

## setup: cria o .env a partir do .env.example, se ainda nao existir
setup:
	@test -f .env || (cp .env.example .env && echo ".env criado a partir do .env.example")

## db-up: sobe o PostgreSQL (Docker) e espera ficar saudavel
db-up: setup
	docker compose up -d --wait

## db-down: para o banco (os dados sao mantidos)
db-down:
	docker compose down

## stop: alias de db-down
stop: db-down

## db-reset: apaga os dados e recria o banco do zero (schema + seed)
db-reset:
	docker compose down -v
	docker compose up -d --wait

## install-frontend: instala as dependencias do frontend (so se faltar)
install-frontend:
	@test -d frontend/node_modules || (cd frontend && npm install)

## backend: roda o backend Spring Boot (porta 8080)
backend: setup
	cd backend && mvn spring-boot:run

## bff: roda o BFF Spring Boot (porta 3000)
bff: setup
	cd bff && mvn spring-boot:run

## frontend: roda o frontend Vue (porta 5173)
frontend: install-frontend
	cd frontend && npm run dev

## run: sobe banco, backend, BFF e frontend juntos (Ctrl+C encerra todos)
run: db-up
	@echo ">> Backend:  http://localhost:8080/actuator/health"
	@echo ">> BFF:      http://localhost:3000/actuator/health"
	@echo ">> Frontend: http://localhost:5173"
	$(MAKE) -j3 backend bff frontend

## help: lista os comandos
help:
	@grep -E "^## " Makefile | sed "s/^## /  make /"