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
| `equipment` | `Equipment` | Catalog thiết bị |
| `room_equipment` | `RoomEquipment` | Thiết bị gắn phòng |
| `equipment_purchase` | `EquipmentPurchase` | Yêu cầu mua / fulfill |

API handoff chi tiết (payloads, roles, enrichment): FE `docs/be-fe-contract.html`.

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
| `name` | `varchar(50)` | NO | PK. `ROLE_ADMIN` \| `ROLE_MANAGER` \| `ROLE_STAFF` \| `ROLE_USER` (hierarchy ADMIN > MANAGER > STAFF > USER) | `name` |

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
| `is_vip` | `boolean` | NO | default `false` | `isVip` |
| `vip_amenities` | `varchar(500)` | YES | CSV codes: `VIDEO_4K`,`SOUNDPROOF`,`CATERING`,`DEDICATED_SUPPORT`,`PRIVACY_GLASS` | `vipAmenities` |
| `layout_type` | `varchar(20)` | NO | Enum `RoomLayoutType`: `COMPACT` \| `STANDARD` \| `SPACIOUS` \| `BOARDROOM` \| `AUDITORIUM`. Độc lập `capacity`. Default write `STANDARD`. | `layoutType` |
| `floor_width_m` | `decimal(5,2)` | NO | ≥ 1.0 m. Default write `6.00`. | `floorWidthM` |
| `floor_depth_m` | `decimal(5,2)` | NO | ≥ 1.0 m. Default write `4.50`. | `floorDepthM` |

Không persist: `RoomDTO.equipmentCategories[]`, `RoomDTO.equipmentNames[]` (tính từ `room_equipment` `status=OK`).

Layout / floor size độc lập với `capacity`.

### RoomDTO enrichment (không persist)

Trên `GET/POST/PUT/PATCH /api/rooms*`, service gắn từ `room_equipment` (`status = OK`):

| Field | Nghĩa | Dùng cho |
|-------|--------|----------|
| `equipmentCategories[]` | Distinct `equipment.category` | Query `?equipmentCategory=` (AND) |
| `equipmentNames[]` | Distinct `equipment.name` | Label UI (timeline, Select) — **không** dùng category (“OTHER” ≠ “Micro không dây”) |

Chi tiết inventory (qty/status): `GET /api/rooms/{id}/equipment`.

---

## 6. `booking` (`Booking`)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `title` | `varchar(200)` | NO | | `title` |
| `start_time` | `timestamp` | NO | Instant | `startTime` |
| `end_time` | `timestamp` | NO | Instant | `endTime` |
| `status` | `varchar(255)` | NO | Enum `BookingStatus`: `PENDING` \| `APPROVED` \| `CANCELLED` \| `EXPIRED` | `status` |
| `room_id` | `bigint` | NO | FK → `room.id` | `room` |
| `user_id` | `bigint` | NO | FK → `jhi_user.id` | `user` |
| `price_per_hour` | `decimal(19,2)` | NO | snapshot lúc tạo | `pricePerHour` |
| `amount` | `decimal(19,2)` | NO | snapshot tổng tiền | `amount` |
| `payment_status` | `varchar(20)` | YES | Enum `PaymentStatus`: `UNPAID` \| `PAID`. Null khi PENDING. | `paymentStatus` |
| `approved_by_id` | `bigint` | YES | FK → `jhi_user.id` | `approvedBy` |

---

## 7. `department_change_request` (`DepartmentChangeRequest` + audit)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `user_id` | `bigint` | NO | FK → `jhi_user.id` | `user` |
| `requested_department_id` | `bigint` | NO | FK → `department.id` | `requestedDepartment` |
| `status` | `varchar(20)` | NO | default `PENDING`. Enum `DepartmentChangeRequestStatus`: `PENDING` \| `APPROVED` \| `REJECTED` | `status` |
| `reviewed_by_id` | `bigint` | YES | FK → `jhi_user.id` | `reviewedBy` |
| `reviewed_date` | `timestamp` | YES | | `reviewedDate` |
| `created_by` | `varchar(50)` | NO | audit | `createdBy` |
| `created_date` | `timestamp` | YES | audit | `createdDate` |
| `last_modified_by` | `varchar(50)` | YES | audit | `lastModifiedBy` |
| `last_modified_date` | `timestamp` | YES | audit | `lastModifiedDate` |

---

## 8. `notification` (`Notification` + audit)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `user_id` | `bigint` | NO | FK → `jhi_user.id` | `user` |
| `type` | `varchar(50)` | NO | Enum `NotificationType`: `BOOKING_PENDING` \| `BOOKING_APPROVED` \| `BOOKING_REJECTED` \| `BOOKING_CANCELLED` \| `BOOKING_EXPIRED` \| `DEPT_CHANGE_PENDING` \| `DEPT_CHANGE_APPROVED` \| `DEPT_CHANGE_REJECTED` | `type` |
| `title` | `varchar(200)` | NO | | `title` |
| `message` | `varchar(500)` | NO | | `message` |
| `booking_id` | `bigint` | YES | **polymorphic** — booking id **hoặc** dept-change-request id; **không** FK → `booking` | `bookingId` |
| `read_date` | `timestamp` | YES | `null` = chưa đọc | `readDate` |
| `created_by` | `varchar(50)` | NO | audit | `createdBy` |
| `created_date` | `timestamp` | YES | audit | `createdDate` |
| `last_modified_by` | `varchar(50)` | YES | audit | `lastModifiedBy` |
| `last_modified_date` | `timestamp` | YES | audit | `lastModifiedDate` |

Index: `idx_notification_user_created` (`user_id`, `created_date`).

---

## 9. `equipment` (`Equipment`)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `name` | `varchar(100)` | NO | display / `equipmentNames` | `name` |
| `category` | `varchar(20)` | NO | Enum `EquipmentCategory`: `PROJECTOR` \| `DISPLAY` \| `AUDIO` \| `VC` \| `MICROPHONE` \| `OTHER` | `category` |
| `unit_cost` | `decimal(19,2)` | NO | | `unitCost` |
| `is_active` | `boolean` | NO | | `isActive` |

Seed catalog (ví dụ): Máy chiếu Full HD (`PROJECTOR`), Màn hình LED 55" (`DISPLAY`), Loa hội nghị (`AUDIO`), Camera họp trực tuyến (`VC`), Micro không dây (`MICROPHONE`).

---

## 10. `room_equipment` (`RoomEquipment`)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `room_id` | `bigint` | NO | FK → `room.id`; UNIQUE(`room_id`,`equipment_id`) | `room` |
| `equipment_id` | `bigint` | NO | FK → `equipment.id` | `equipment` |
| `quantity` | `integer` | NO | | `quantity` |
| `status` | `varchar(20)` | NO | Enum `RoomEquipmentStatus`: `OK` \| `BROKEN` \| `RETIRED`. Chỉ `OK` vào filter / `equipmentCategories` / `equipmentNames`. | `status` |

---

## 11. `equipment_purchase` (`EquipmentPurchase` + audit)

| Column | Type | Null | Constraint | Java field |
|--------|------|------|------------|------------|
| `id` | `bigint` | NO | PK | `id` |
| `room_id` | `bigint` | NO | FK → `room` | `room` |
| `equipment_id` | `bigint` | NO | FK → `equipment` | `equipment` |
| `quantity` | `integer` | NO | | `quantity` |
| `unit_cost` | `decimal(19,2)` | YES | | `unitCost` |
| `reason` | `varchar(500)` | YES | | `reason` |
| `status` | `varchar(20)` | NO | Enum `PurchaseStatus`: `PENDING` \| `APPROVED` \| `REJECTED` \| `FULFILLED` | `status` |
| `requested_by_id` | `bigint` | NO | FK → `jhi_user` | `requestedBy` |
| `approved_by_id` | `bigint` | YES | FK → `jhi_user` | `approvedBy` |
| `fulfilled_at` | `timestamp` | YES | | `fulfilledAt` |
| audit | … | | | |

Fulfill cập nhật / tạo `room_equipment` quantity.

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
room 1──* room_equipment *──1 equipment
room 1──* equipment_purchase *──1 equipment
```

`notification.booking_id` là ref đa hình (booking **hoặc** department_change_request) — không FK cứng sang `booking`.

## Audit fields (`AbstractAuditingEntity`)

Áp dụng cho: `jhi_user`, `department_change_request`, `notification`, `equipment_purchase`.

| Column | Type | Null |
|--------|------|------|
| `created_by` | `varchar(50)` | NO |
| `created_date` | `timestamp` | YES |
| `last_modified_by` | `varchar(50)` | YES |
| `last_modified_date` | `timestamp` | YES |
