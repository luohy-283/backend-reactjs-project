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
          and booking.status in (
            com.company.bookingroom.domain.enumeration.BookingStatus.PENDING,
            com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
          )
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
     * Active (PENDING / APPROVED) bookings overlapping a local calendar day range.
     */
    @Query(
        value = """
            select booking from Booking booking
            left join fetch booking.room
            left join fetch booking.user
            where booking.startTime < :dayEnd
              and booking.endTime > :dayStart
              and booking.status in (
                com.company.bookingroom.domain.enumeration.BookingStatus.PENDING,
                com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
              )
            """,
        countQuery = """
            select count(booking) from Booking booking
            where booking.startTime < :dayEnd
              and booking.endTime > :dayStart
              and booking.status in (
                com.company.bookingroom.domain.enumeration.BookingStatus.PENDING,
                com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
              )
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

    /**
     * Visible bookings with optional status, calendar day, upcoming (APPROVED + startTime &gt; now),
     * and text search on title / room name / user login·email·fullName.
     */
    @Query(
        value = """
            select booking from Booking booking
            left join fetch booking.room r
            left join fetch r.lockedDepartment
            left join fetch booking.user u
            where (:isAdmin = true
              or r.lockedDepartment is null
              or (:departmentId is not null and r.lockedDepartment.id = :departmentId))
              and (:status is null or booking.status = :status)
              and (:hasDate = false or (booking.startTime < :dayEnd and booking.endTime > :dayStart))
              and (:activeOnly = false or booking.status in (
                com.company.bookingroom.domain.enumeration.BookingStatus.PENDING,
                com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
              ))
              and (:upcoming = false or (
                booking.status = com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
                and booking.startTime > :now
              ))
              and (:q is null or (
                lower(booking.title) like lower(concat('%', cast(:q as string), '%'))
                or lower(r.name) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.login) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.email) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.fullName) like lower(concat('%', cast(:q as string), '%'))
              ))
            """,
        countQuery = """
            select count(booking) from Booking booking
            left join booking.room r
            left join booking.user u
            where (:isAdmin = true
              or r.lockedDepartment is null
              or (:departmentId is not null and r.lockedDepartment.id = :departmentId))
              and (:status is null or booking.status = :status)
              and (:hasDate = false or (booking.startTime < :dayEnd and booking.endTime > :dayStart))
              and (:activeOnly = false or booking.status in (
                com.company.bookingroom.domain.enumeration.BookingStatus.PENDING,
                com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
              ))
              and (:upcoming = false or (
                booking.status = com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
                and booking.startTime > :now
              ))
              and (:q is null or (
                lower(booking.title) like lower(concat('%', cast(:q as string), '%'))
                or lower(r.name) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.login) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.email) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.fullName) like lower(concat('%', cast(:q as string), '%'))
              ))
            """
    )
    Page<Booking> findVisibleFiltered(
        @Param("status") BookingStatus status,
        @Param("hasDate") boolean hasDate,
        @Param("dayStart") Instant dayStart,
        @Param("dayEnd") Instant dayEnd,
        @Param("activeOnly") boolean activeOnly,
        @Param("upcoming") boolean upcoming,
        @Param("now") Instant now,
        @Param("q") String q,
        @Param("isAdmin") boolean isAdmin,
        @Param("departmentId") Long departmentId,
        Pageable pageable
    );

    @Query(
        value = """
            select booking from Booking booking
            left join fetch booking.room r
            left join fetch booking.user
            where booking.user.login = :login
              and booking.status = com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
              and (:q is null or (
                lower(booking.title) like lower(concat('%', :q, '%'))
                or lower(r.name) like lower(concat('%', :q, '%'))
              ))
            """,
        countQuery = """
            select count(booking) from Booking booking
            left join booking.room r
            where booking.user.login = :login
              and booking.status = com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
              and (:q is null or (
                lower(booking.title) like lower(concat('%', :q, '%'))
                or lower(r.name) like lower(concat('%', :q, '%'))
              ))
            """
    )
    Page<Booking> findApprovedInvoicesByLogin(
        @Param("login") String login,
        @Param("q") String q,
        Pageable pageable
    );

    @Query(
        """
        select booking from Booking booking
        left join fetch booking.room
        left join fetch booking.user
        where booking.status = com.company.bookingroom.domain.enumeration.BookingStatus.PENDING
          and booking.startTime <= :now
        """
    )
    List<Booking> findPendingStartingAtOrBefore(@Param("now") Instant now);

    @Query(
        """
        select booking from Booking booking
        left join fetch booking.room
        left join fetch booking.user
        where booking.id = :id
          and booking.user.login = :login
          and booking.status = com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
        """
    )
    Optional<Booking> findApprovedInvoiceByIdAndLogin(@Param("id") Long id, @Param("login") String login);

    @Query(
        """
        select booking from Booking booking
        left join fetch booking.room
        where booking.status = com.company.bookingroom.domain.enumeration.BookingStatus.APPROVED
          and booking.startTime >= :monthStart
          and booking.startTime < :monthEnd
        order by booking.startTime asc
        """
    )
    List<Booking> findApprovedInMonth(@Param("monthStart") Instant monthStart, @Param("monthEnd") Instant monthEnd);

    @Query(
        """
        select count(booking) from Booking booking
        where booking.status = com.company.bookingroom.domain.enumeration.BookingStatus.CANCELLED
          and booking.startTime >= :monthStart
          and booking.startTime < :monthEnd
        """
    )
    long countCancelledInMonth(@Param("monthStart") Instant monthStart, @Param("monthEnd") Instant monthEnd);
}
