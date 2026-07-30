package com.company.bookingroom.repository;

import com.company.bookingroom.domain.Room;
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
     * Optionally only active rooms.
     */
    @Query(
        value = """
        select room from Room room
        left join fetch room.lockedDepartment
        where (:onlyActive = false or room.isActive = true)
          and (
            room.lockedDepartment is null
            or (:departmentId is not null and room.lockedDepartment.id = :departmentId)
          )
        """,
        countQuery = """
        select count(room) from Room room
        where (:onlyActive = false or room.isActive = true)
          and (
            room.lockedDepartment is null
            or (:departmentId is not null and room.lockedDepartment.id = :departmentId)
          )
        """
    )
    Page<Room> findVisibleForDepartment(
        @Param("departmentId") Long departmentId,
        @Param("onlyActive") boolean onlyActive,
        Pageable pageable
    );

    @Query(
        value = """
            select room from Room room
            left join fetch room.lockedDepartment
            where (:onlyActive = false or room.isActive = true)
            """,
        countQuery = """
            select count(room) from Room room
            where (:onlyActive = false or room.isActive = true)
            """
    )
    Page<Room> findAllWithDepartment(@Param("onlyActive") boolean onlyActive, Pageable pageable);
}
