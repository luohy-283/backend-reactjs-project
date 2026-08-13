package com.company.bookingroom.repository;

import com.company.bookingroom.domain.EquipmentPurchase;
import com.company.bookingroom.domain.enumeration.PurchaseStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentPurchaseRepository extends JpaRepository<EquipmentPurchase, Long> {
    @Query(
        value = """
            select p from EquipmentPurchase p
            join fetch p.room
            join fetch p.equipment
            left join fetch p.requestedBy
            left join fetch p.approvedBy
            where (:status is null or p.status = :status)
              and (:roomId is null or p.room.id = :roomId)
            """,
        countQuery = """
            select count(p) from EquipmentPurchase p
            where (:status is null or p.status = :status)
              and (:roomId is null or p.room.id = :roomId)
            """
    )
    Page<EquipmentPurchase> findFiltered(
        @Param("status") PurchaseStatus status,
        @Param("roomId") Long roomId,
        Pageable pageable
    );

    @Query(
        """
        select p from EquipmentPurchase p
        join fetch p.room
        where p.status = com.company.bookingroom.domain.enumeration.PurchaseStatus.FULFILLED
          and p.fulfilledAt >= :from
          and p.fulfilledAt < :to
        """
    )
    List<EquipmentPurchase> findFulfilledInRange(@Param("from") Instant from, @Param("to") Instant to);
}
