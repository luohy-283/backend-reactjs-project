package com.company.bookingroom.repository;

import com.company.bookingroom.domain.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    @Query(
        """
        select e from Equipment e
        where (:active is null or e.isActive = :active)
          and (
            :q is null
            or lower(e.name) like lower(concat('%', cast(:q as string), '%'))
            or lower(cast(e.category as string)) like lower(concat('%', cast(:q as string), '%'))
          )
        """
    )
    Page<Equipment> findAllFiltered(@Param("active") Boolean active, @Param("q") String q, Pageable pageable);
}
