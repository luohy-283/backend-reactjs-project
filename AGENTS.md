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
- Entities: `Room`, `Booking` (status: PENDING / APPROVED / CANCELLED), `Department`, `DepartmentChangeRequest`
- Spec login: `POST /api/auth/login` `{ email, password }` → `{ token, user: { id, email, fullName, role, department } }`
- `jhi_user` slim: `login`(=email), `password_hash`, `full_name`, `email`, `activated`, `department_id`; role via `jhi_user_authority` (no register/mail/activation)
- Room: optional `locked_department_id` (null = public). USER list/book only public + own dept; ADMIN sees all. `price_per_hour` (VND/hour)
- Booking: snapshot `price_per_hour` + `amount` on create; `amount` = price × ceil(duration/30min)×0.5h (min 1 block); invoices = APPROVED (`GET /api/account/invoices`)
- Admin revenue: `GET /api/admin/revenue?yearMonth=yyyy-MM` — KPIs + vs previous month, byRoom (share%), byDay (full month)
- Room write APIs: ADMIN only; soft-delete via `isActive=false`
- Booking create: USER→PENDING, ADMIN→APPROVED; overlap check excludes CANCELLED; room access guard
- Admin approve/reject: `POST /api/bookings/{id}/approve|reject`
- Account: `GET|PUT /api/account` (fullName/email); department change via `/api/account/department-change-requests` + admin approve
- Departments: `GET /api/departments`; admin users: `/api/admin/users`

## Config notes

- Docker Compose disabled (`spring.docker.compose.enabled=false`)
- CORS allows `http://localhost:5173` and `http://*:5173` (LAN / cross-machine dev)
- Seed users: `admin@company.com` / `user@company.com`, password `123456`
- Demo seed (`20260730190000_reseed_demo_data`): 28 rooms, 70 bookings, +20 users (`user3…22@company.com` / `123456`) for pagination

## Forbidden

- Re-enabling Docker Compose for local DB without asking
- Hard-deleting rooms (use soft deactivate)
