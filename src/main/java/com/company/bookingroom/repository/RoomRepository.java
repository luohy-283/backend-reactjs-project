package com.company.bookingroom.repository;

import com.company.bookingroom.domain.Room;
import com.company.bookingroom.domain.enumeration.EquipmentCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Room entity.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    /**
     * Rooms visible to a non-admin user: public OR locked to their department.
     * Optional {@code active} / {@code vip} filters ({@code null} = all). Optional text {@code q}.
     * When {@code equipmentCategoryCount > 0}, room must have OK inventory covering all listed categories (AND).
     */
    @Query(
        value = """
        select room from Room room
        left join fetch room.lockedDepartment dept
        where (:active is null or room.isActive = :active)
          and (:vip is null or room.isVip = :vip)
          and (
            room.lockedDepartment is null
            or (:departmentId is not null and room.lockedDepartment.id = :departmentId)
          )
          and (
            :q is null
            or lower(room.name) like lower(concat('%', cast(:q as string), '%'))
            or cast(room.capacity as string) like concat('%', cast(:q as string), '%')
            or (dept is not null and (
              lower(dept.name) like lower(concat('%', cast(:q as string), '%'))
              or lower(dept.code) like lower(concat('%', cast(:q as string), '%'))
            ))
          )
          and (
            :equipmentCategoryCount = 0
            or (
              select count(distinct eq.category)
              from RoomEquipment re
              join re.equipment eq
              where re.room = room
                and re.status = com.company.bookingroom.domain.enumeration.RoomEquipmentStatus.OK
                and eq.category in :equipmentCategories
            ) = :equipmentCategoryCount
          )
        """,
        countQuery = """
        select count(room) from Room room
        left join room.lockedDepartment dept
        where (:active is null or room.isActive = :active)
          and (:vip is null or room.isVip = :vip)
          and (
            room.lockedDepartment is null
            or (:departmentId is not null and room.lockedDepartment.id = :departmentId)
          )
          and (
            :q is null
            or lower(room.name) like lower(concat('%', cast(:q as string), '%'))
            or cast(room.capacity as string) like concat('%', cast(:q as string), '%')
            or (dept is not null and (
              lower(dept.name) like lower(concat('%', cast(:q as string), '%'))
              or lower(dept.code) like lower(concat('%', cast(:q as string), '%'))
            ))
          )
          and (
            :equipmentCategoryCount = 0
            or (
              select count(distinct eq.category)
              from RoomEquipment re
              join re.equipment eq
              where re.room = room
                and re.status = com.company.bookingroom.domain.enumeration.RoomEquipmentStatus.OK
                and eq.category in :equipmentCategories
            ) = :equipmentCategoryCount
          )
        """
    )
    Page<Room> findVisibleForDepartment(
        @Param("departmentId") Long departmentId,
        @Param("active") Boolean active,
        @Param("vip") Boolean vip,
        @Param("q") String q,
        @Param("equipmentCategories") Collection<EquipmentCategory> equipmentCategories,
        @Param("equipmentCategoryCount") long equipmentCategoryCount,
        Pageable pageable
    );

    @Query(
        value = """
            select room from Room room
            left join fetch room.lockedDepartment dept
            where (:active is null or room.isActive = :active)
              and (:vip is null or room.isVip = :vip)
              and (
                :q is null
                or lower(room.name) like lower(concat('%', cast(:q as string), '%'))
                or cast(room.capacity as string) like concat('%', cast(:q as string), '%')
                or (dept is not null and (
                  lower(dept.name) like lower(concat('%', cast(:q as string), '%'))
                  or lower(dept.code) like lower(concat('%', cast(:q as string), '%'))
                ))
              )
              and (
                :equipmentCategoryCount = 0
                or (
                  select count(distinct eq.category)
                  from RoomEquipment re
                  join re.equipment eq
                  where re.room = room
                    and re.status = com.company.bookingroom.domain.enumeration.RoomEquipmentStatus.OK
                    and eq.category in :equipmentCategories
                ) = :equipmentCategoryCount
              )
            """,
        countQuery = """
            select count(room) from Room room
            left join room.lockedDepartment dept
            where (:active is null or room.isActive = :active)
              and (:vip is null or room.isVip = :vip)
              and (
                :q is null
                or lower(room.name) like lower(concat('%', cast(:q as string), '%'))
                or cast(room.capacity as string) like concat('%', cast(:q as string), '%')
                or (dept is not null and (
                  lower(dept.name) like lower(concat('%', cast(:q as string), '%'))
                  or lower(dept.code) like lower(concat('%', cast(:q as string), '%'))
                ))
              )
              and (
                :equipmentCategoryCount = 0
                or (
                  select count(distinct eq.category)
                  from RoomEquipment re
                  join re.equipment eq
                  where re.room = room
                    and re.status = com.company.bookingroom.domain.enumeration.RoomEquipmentStatus.OK
                    and eq.category in :equipmentCategories
                ) = :equipmentCategoryCount
              )
            """
    )
    Page<Room> findAllWithDepartment(
        @Param("active") Boolean active,
        @Param("vip") Boolean vip,
        @Param("q") String q,
        @Param("equipmentCategories") Collection<EquipmentCategory> equipmentCategories,
        @Param("equipmentCategoryCount") long equipmentCategoryCount,
        Pageable pageable
    );
}
