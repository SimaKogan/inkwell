.PHONY: up down logs test db

up:            ## build and start everything on http://localhost:3000
	@test -f .env || cp .env.example .env
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f backend ml

db:            ## only PostgreSQL, for running backend/frontend locally
	@test -f .env || cp .env.example .env
	docker compose up -d db

test:
	cd backend && mvn -B verify
	cd frontend && npm ci && npm run build
	cd ml-service && pip install -r requirements-dev.txt && python -m pytest -q tests
