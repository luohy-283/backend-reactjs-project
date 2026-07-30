# AGENTS.md

Project-specific instructions for coding agents (JHipster backend).

## Stack

- JHipster 9.1 / Spring Boot 4 / Java / Maven / JWT
- PostgreSQL local (no Docker Compose): DB `meeting_room_booking`, user `postgres`
- Package: `com.company.bookingroom`
- No client (`skipClient`)

## Commands

| Purpose | Command |
| ------- | ------- |
| Run (dev) | `./mvnw` or `mvnw.cmd` |
| Test | `./mvnw verify` |
| Generate entities | `npx jhipster jdl meeting-room.jdl` |

## Domain

- Built-in `jhi_user` + authorities `ROLE_ADMIN` / `ROLE_USER`
- Entities: `Room`, `Booking` (status: PENDING / APPROVED / CANCELLED)
- Spec login: `POST /api/auth/login` `{ email, password }` → `{ token, user: { id, email, fullName, role } }`
- `jhi_user` slim: `login`(=email), `password_hash`, `full_name`, `email`, `activated`; role via `jhi_user_authority` (no register/mail/activation)
- Room write APIs: ADMIN only; soft-delete via `isActive=false`
- Booking create: USER→PENDING, ADMIN→APPROVED; overlap check excludes CANCELLED
- Admin approve/reject: `POST /api/bookings/{id}/approve|reject`

## Config notes

- Docker Compose disabled (`spring.docker.compose.enabled=false`)
- CORS allows `http://localhost:5173` and `http://*:5173` (LAN / cross-machine dev)
- Seed users: `admin@company.com` / `user@company.com`, password `123456`

## Forbidden

- Re-enabling Docker Compose for local DB without asking
- Hard-deleting rooms (use soft deactivate)
