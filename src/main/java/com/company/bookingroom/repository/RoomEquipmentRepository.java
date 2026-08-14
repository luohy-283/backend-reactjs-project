package com.company.bookingroom.repository;

import com.company.bookingroom.domain.RoomEquipment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomEquipmentRepository extends JpaRepository<RoomEquipment, Long> {
    @Query(
        """
        select re from RoomEquipment re
        join fetch re.equipment
        where re.room.id = :roomId
        order by re.equipment.name
        """
    )
    List<RoomEquipment> findByRoomIdWithEquipment(@Param("roomId") Long roomId);

    Optional<RoomEquipment> findByIdAndRoomId(Long id, Long roomId);

    Optional<RoomEquipment> findByRoomIdAndEquipmentId(Long roomId, Long equipmentId);

    @Query(
        """
        select re.room.id, eq.category, eq.name
        from RoomEquipment re
        join re.equipment eq
        where re.room.id in :roomIds
          and re.status = com.company.bookingroom.domain.enumeration.RoomEquipmentStatus.OK
        order by eq.name
        """
    )
    List<Object[]> findOkEquipmentByRoomIds(@Param("roomIds") Collection<Long> roomIds);
}
