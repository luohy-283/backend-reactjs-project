package com.company.bookingroom.repository;

import com.company.bookingroom.domain.Notification;
import com.company.bookingroom.domain.enumeration.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserLoginOrderByCreatedDateDesc(String login, Pageable pageable);

    long countByUserLoginAndReadDateIsNull(String login);

    // flushAutomatically: pending entity UPDATEs (booking/dept-change status) must hit the DB
    // before clearAutomatically wipes the persistence context — otherwise approve returns 200
    // with the in-memory DTO while the transaction commits without those UPDATEs.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        update Notification n
        set n.readDate = CURRENT_TIMESTAMP
        where n.user.login = :login and n.readDate is null
        """
    )
    int markAllReadForLogin(@Param("login") String login);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        delete from Notification n
        where n.type = :type and n.bookingId = :refId
        """
    )
    int deleteByTypeAndRefId(@Param("type") NotificationType type, @Param("refId") Long refId);
}
