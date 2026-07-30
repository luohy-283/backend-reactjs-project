package com.company.bookingroom.repository;

import com.company.bookingroom.domain.Booking;
import com.company.bookingroom.domain.enumeration.BookingStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Booking entity.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("select booking from Booking booking where booking.user.login = ?#{authentication.name}")
    List<Booking> findByUserIsCurrentUser();

    default Optional<Booking> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Booking> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Booking> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select booking from Booking booking left join fetch booking.room left join fetch booking.user",
        countQuery = "select count(booking) from Booking booking"
    )
    Page<Booking> findAllWithToOneRelationships(Pageable pageable);

    @Query("select booking from Booking booking left join fetch booking.room left join fetch booking.user")
    List<Booking> findAllWithToOneRelationships();

    @Query("select booking from Booking booking left join fetch booking.room left join fetch booking.user where booking.id =:id")
    Optional<Booking> findOneWithToOneRelationships(@Param("id") Long id);

    @Query(
        """
        select case when count(booking) > 0 then true else false end
        from Booking booking
        where booking.room.id = :roomId
          and booking.status <> com.company.bookingroom.domain.enumeration.BookingStatus.CANCELLED
          and booking.startTime < :endTime
          and booking.endTime > :startTime
          and (:excludeId is null or booking.id <> :excludeId)
        """
    )
    boolean existsOverlapping(
        @Param("roomId") Long roomId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("excludeId") Long excludeId
    );

    /**
     * Active (non-CANCELLED) bookings overlapping a local calendar day range.
     */
    @Query(
        value = """
            select booking from Booking booking
            left join fetch booking.room
            left join fetch booking.user
            where booking.startTime < :dayEnd
              and booking.endTime > :dayStart
              and booking.status <> com.company.bookingroom.domain.enumeration.BookingStatus.CANCELLED
            """,
        countQuery = """
            select count(booking) from Booking booking
            where booking.startTime < :dayEnd
              and booking.endTime > :dayStart
              and booking.status <> com.company.bookingroom.domain.enumeration.BookingStatus.CANCELLED
            """
    )
    Page<Booking> findActiveByDayRange(
        @Param("dayStart") Instant dayStart,
        @Param("dayEnd") Instant dayEnd,
        Pageable pageable
    );

    @Query(
        value = """
            select booking from Booking booking
            left join fetch booking.room
            left join fetch booking.user
            where booking.status = :status
            """,
        countQuery = "select count(booking) from Booking booking where booking.status = :status"
    )
    Page<Booking> findAllByStatus(@Param("status") BookingStatus status, Pageable pageable);

    @Query(
        value = """
            select booking from Booking booking
            left join fetch booking.room
            left join fetch booking.user
            where booking.status = :status
              and booking.startTime < :dayEnd
              and booking.endTime > :dayStart
            """,
        countQuery = """
            select count(booking) from Booking booking
            where booking.status = :status
              and booking.startTime < :dayEnd
              and booking.endTime > :dayStart
            """
    )
    Page<Booking> findByDayRangeAndStatus(
        @Param("dayStart") Instant dayStart,
        @Param("dayEnd") Instant dayEnd,
        @Param("status") BookingStatus status,
        Pageable pageable
    );

    @Query(
        """
        select case when count(booking) > 0 then true else false end
        from Booking booking
        where booking.room.id = :roomId
          and booking.status in (
            com.company.bookingroom.domain.enumeration.BookingStatus.PENDING,
            com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
          )
          and booking.endTime > :now
        """
    )
    boolean existsActiveBookingsForRoom(@Param("roomId") Long roomId, @Param("now") Instant now);
}
