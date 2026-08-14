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

- Built-in `jhi_user` + authorities `ROLE_ADMIN` / `ROLE_MANAGER` / `ROLE_STAFF` / `ROLE_USER`
- Entities: `Room`, `Booking`, `Department`, `DepartmentChangeRequest`, `Equipment`, `RoomEquipment`, `EquipmentPurchase`, `Notification`
- Spec login: `POST /api/auth/login` `{ email, password }` → `{ token, user: { id, email, fullName, role, department } }`
- `jhi_user` slim: `login`(=email), `password_hash`, `full_name`, `email`, `activated`, `department_id`; role via `jhi_user_authority` (no register/mail/activation)
- Room: optional `locked_department_id` (null = public). USER list/book only public + own dept; ADMIN/MANAGER see all. `price_per_hour` (VND/hour). VIP: `is_vip` + `vip_amenities` CSV. Layout: `layout_type` + `floor_width_m`/`floor_depth_m` (3D floor; independent of capacity).
- Rooms list: `GET /api/rooms?page&size&sort&q&active&vip&equipmentCategory` — `equipmentCategory` repeat = AND on OK inventory; response enrich `equipmentCategories[]` (filter) + `equipmentNames[]` (UI labels from catalog `name`). Detail inventory: `GET /api/rooms/{id}/equipment`.
- Booking: snapshot `price_per_hour` + `amount` on create; `amount` = price × ceil(duration/30min)×0.5h (min 1 block); invoices = APPROVED (`GET /api/account/invoices` — Pageable + optional `q` on title/room name)
- Admin revenue: `GET /api/admin/revenue?yearMonth=yyyy-MM` — KPIs + vs previous month, byRoom (share%), byDay (full month); paged by-room table: `GET /api/admin/revenue/by-room?yearMonth=&page=&size=&sort=&q=`
- Room write APIs: MANAGER+ (hierarchy); soft-delete via `isActive=false`
- Booking create: USER→PENDING, ADMIN→APPROVED; overlap check excludes CANCELLED/EXPIRED; room access guard
- PENDING past `startTime` → EXPIRED (on list/approve/reject + scheduled every 5 min); history badge “Hết hạn”
- `GET /api/bookings`: `page`/`size`/`sort` + optional `date`, `status`, `q` (title/room name/user login·email·fullName), `upcoming=true` (APPROVED + `startTime` > now)
- Admin approve/reject: `POST /api/bookings/{id}/approve|reject`
- Account: `GET|PUT /api/account` (fullName/email); department change via `/api/account/department-change-requests` + admin approve
- Notifications: `GET /api/notifications`, `GET /api/notifications/unread-count`, `POST .../read`, `POST .../read-all`; created on pending/approve/reject/cancel booking and on department-change create/approve/reject (`DEPT_CHANGE_*`).
- Export CSV: `GET /api/account/invoices/export`, `GET /api/admin/revenue/export?yearMonth=`
- Departments: `GET /api/departments`; admin users: `/api/admin/users` (optional `q`, `activated`; list + `X-Total-Count`)

## Config notes

- Docker Compose disabled (`spring.docker.compose.enabled=false`)
- CORS allows `http://localhost:5173` and `http://*:5173` (LAN / cross-machine dev)
- Seed users: `admin@company.com` / `user@company.com`, password `123456`
- Demo seed (`20260730190000_reseed_demo_data`): 28 rooms, 70 bookings, +20 users (`user3…22@company.com` / `123456`) for pagination

## Forbidden

- Re-enabling Docker Compose for local DB without asking
- Hard-deleting rooms (use soft deactivate)

## Project learnings

- Always filter rooms server-side via `GET /api/rooms` optional `q` + `active` (with Pageable); do not add a second rooms list endpoint for search.
- Always use `cast(:q as string)` in JPQL `concat`/`lower` search predicates on PostgreSQL; bare `:q` binds as bytea and fails with `function lower(bytea) does not exist`.
- Always treat `notification.booking_id` as a polymorphic ref (booking id OR department-change-request id) — do not add an FK to `booking` (blocks DEPT_CHANGE_* inserts).
- Always enrich RoomDTO with `equipmentNames` (catalog `name`) for UI labels and `equipmentCategories` for filters — never display category enums (e.g. OTHER → “Khác”) as product names.
- Always keep `layout_type` + floor meters independent of `capacity`; 3D floor size reads `floorWidthM`/`floorDepthM`.
- Always bind PUT/PATCH room (and equipment) `id` from path when body.id is null; form PATCH with `name` present applies `lockedDepartment` including JSON null (unlock). Status-only PATCH must not clear lock.
