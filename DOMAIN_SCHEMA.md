# Domain Schema — Meeting Room Booking

Nguồn: `com.company.bookingroom.domain.*` + Liquibase changelogs.  
Stack: JHipster / Spring Boot / PostgreSQL. PK số dùng sequence `sequenceGenerator`.

## Overview

| Table | Entity | Ghi chú |
|-------|--------|---------|
| `jhi_user` | `User` | User + audit |
| `jhi_authority` | `Authority` | Role |
| `jhi_user_authority` | (join) | User ↔ Role |
| `department` | `Department` | Phòng ban |
| `room` | `Room` | Phòng họp |
| `booking` | `Booking` | Đặt phòng |
| `department_change_request` | `DepartmentChangeRequest` | Đổi phòng ban |
| `notification` | `Notification` | Thông báo |

---

## 1. `jhi_user` (`User` extends `AbstractAuditingEntity`)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `login` | `varchar(50)` | NO | UNIQUE | `login` (= email, lowercase) |
| `password_hash` | `varchar(60)` | NO | | `password` |
| `full_name` | `varchar(100)` | YES | | `fullName` |
| `email` | `varchar(254)` | YES | UNIQUE | `email` |
| `activated` | `boolean` | NO | default `false` | `activated` |
| `department_id` | `bigint` | YES | FK → `department.id` | `department` |
| `created_by` | `varchar(50)` | NO | audit | `createdBy` |
| `created_date` | `timestamp` | YES | audit | `createdDate` |
| `last_modified_by` | `varchar(50)` | YES | audit | `lastModifiedBy` |
| `last_modified_date` | `timestamp` | YES | audit | `lastModifiedDate` |

Đã drop (slim): `first_name`, `last_name`, `image_url`, `lang_key`, `activation_key`, `reset_key`, `reset_date`.

---

## 2. `jhi_authority` (`Authority`)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `name` | `varchar(50)` | NO | PK | `name` |

Giá trị: `ROLE_ADMIN`, `ROLE_USER`.

---

## 3. `jhi_user_authority` (ManyToMany join)

| Column | Type | Null | Constraint |
|--------|------|------|------------|
| `user_id` | `bigint` | NO | PK, FK → `jhi_user.id` |
| `authority_name` | `varchar(50)` | NO | PK, FK → `jhi_authority.name` |

---

## 4. `department` (`Department`)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `code` | `varchar(50)` | NO | UNIQUE | `code` |
| `name` | `varchar(100)` | NO | | `name` |

Seed: `IT`, `HR`, `SALES`.

---

## 5. `room` (`Room`)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `name` | `varchar(100)` | NO | | `name` |
| `capacity` | `integer` | NO | min 1 | `capacity` |
| `is_active` | `boolean` | NO | soft-delete = `false` | `isActive` |
| `locked_department_id` | `bigint` | YES | FK → `department.id`; `null` = public | `lockedDepartment` |
| `price_per_hour` | `decimal(19,2)` | NO | ≥ 0, VND/giờ | `pricePerHour` |

---

## 6. `booking` (`Booking`)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `title` | `varchar(200)` | NO | | `title` |
| `start_time` | `timestamp` | NO | Instant | `startTime` |
| `end_time` | `timestamp` | NO | Instant | `endTime` |
| `status` | `varchar(255)` | NO | enum STRING | `status` |
| `room_id` | `bigint` | NO | FK → `room.id` | `room` |
| `user_id` | `bigint` | NO | FK → `jhi_user.id` | `user` |
| `price_per_hour` | `decimal(19,2)` | NO | snapshot lúc tạo | `pricePerHour` |
| `amount` | `decimal(19,2)` | NO | snapshot tổng tiền | `amount` |

### `BookingStatus`

`PENDING` | `APPROVED` | `CANCELLED` | `EXPIRED`

---

## 7. `department_change_request` (`DepartmentChangeRequest` + audit)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `user_id` | `bigint` | NO | FK → `jhi_user.id` | `user` |
| `requested_department_id` | `bigint` | NO | FK → `department.id` | `requestedDepartment` |
| `status` | `varchar(20)` | NO | default `PENDING` | `status` |
| `reviewed_by_id` | `bigint` | YES | FK → `jhi_user.id` | `reviewedBy` |
| `reviewed_date` | `timestamp` | YES | | `reviewedDate` |
| `created_by` | `varchar(50)` | NO | audit | `createdBy` |
| `created_date` | `timestamp` | YES | audit | `createdDate` |
| `last_modified_by` | `varchar(50)` | YES | audit | `lastModifiedBy` |
| `last_modified_date` | `timestamp` | YES | audit | `lastModifiedDate` |

### `DepartmentChangeRequestStatus`

`PENDING` | `APPROVED` | `REJECTED`

---

## 8. `notification` (`Notification` + audit)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `user_id` | `bigint` | NO | FK → `jhi_user.id` | `user` |
| `type` | `varchar(50)` | NO | enum STRING | `type` |
| `title` | `varchar(200)` | NO | | `title` |
| `message` | `varchar(500)` | NO | | `message` |
| `booking_id` | `bigint` | YES | FK → `booking.id` | `bookingId` |
| `read_date` | `timestamp` | YES | `null` = chưa đọc | `readDate` |
| `created_by` | `varchar(50)` | NO | audit | `createdBy` |
| `created_date` | `timestamp` | YES | audit | `createdDate` |
| `last_modified_by` | `varchar(50)` | YES | audit | `lastModifiedBy` |
| `last_modified_date` | `timestamp` | YES | audit | `lastModifiedDate` |

Index: `idx_notification_user_created` (`user_id`, `created_date`).

### `NotificationType`

`BOOKING_PENDING` | `BOOKING_APPROVED` | `BOOKING_REJECTED` | `BOOKING_CANCELLED` | `BOOKING_EXPIRED` | `DEPT_CHANGE_PENDING` | `DEPT_CHANGE_APPROVED` | `DEPT_CHANGE_REJECTED`

---

## Quan hệ (ER tóm tắt)

```
department 1──* jhi_user
department 1──* room (locked_department_id, optional)
jhi_user *──* jhi_authority  (qua jhi_user_authority)
jhi_user 1──* booking
room 1──* booking
jhi_user 1──* department_change_request
department 1──* department_change_request (requested)
jhi_user 1──* notification
booking 1──* notification (optional)
```

## Audit fields (`AbstractAuditingEntity`)

Áp dụng cho: `jhi_user`, `department_change_request`, `notification`.

| Column | Type | Null |
|--------|------|------|
| `created_by` | `varchar(50)` | NO |
| `created_date` | `timestamp` | YES |
| `last_modified_by` | `varchar(50)` | YES |
| `last_modified_date` | `timestamp` | YES |
