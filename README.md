# RoomLedger

Shared-expense tracker for roommates: create a group, add shared expenses, see who owes whom and settle debts. The app is a ledger — it records payments made outside it, it does not move money.

> Work in progress.

## Tech stack

- **Backend:** Java 21, Spring Boot 3.5, Spring Data JPA, PostgreSQL 16, Flyway
- **Frontend:** React, TypeScript, Vite, TanStack Query, Tailwind CSS
- **Testing:** JUnit 5, Testcontainers, Vitest, Testing Library
- **DevOps:** Docker, Docker Compose, GitHub Actions

## Run locally

Requirements: Docker.

```bash
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost (port set by `FRONTEND_PORT` in `.env`)
- Backend health: http://localhost:8080/actuator/health

### Development without Docker for the apps

```bash
docker compose up -d postgres

# terminal 1
cd backend && ./mvnw spring-boot:run

# terminal 2
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## Tests

```bash
cd backend && ./mvnw verify      # needs Docker running (Testcontainers)
cd frontend && npm run lint && npm run test
```
