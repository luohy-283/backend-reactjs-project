package com.company.bookingroom.service;

import com.company.bookingroom.domain.Room;
import com.company.bookingroom.domain.RoomEquipment;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.domain.enumeration.RoomEquipmentStatus;
import com.company.bookingroom.repository.RoomEquipmentRepository;
import com.company.bookingroom.repository.RoomRepository;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.SecurityUtils;
import com.company.bookingroom.service.dto.RoomEquipmentDTO;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoomEquipmentService {

    private static final Logger LOG = LoggerFactory.getLogger(RoomEquipmentService.class);
    private static final String ENTITY_NAME = "roomEquipment";

    private final RoomEquipmentRepository roomEquipmentRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public RoomEquipmentService(
        RoomEquipmentRepository roomEquipmentRepository,
        RoomRepository roomRepository,
        UserRepository userRepository
    ) {
        this.roomEquipmentRepository = roomEquipmentRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomEquipmentDTO> findByRoom(Long roomId) {
        Room room = requireAccessibleRoom(roomId);
        return roomEquipmentRepository.findByRoomIdWithEquipment(room.getId()).stream().map(this::toDto).toList();
    }

    public RoomEquipmentDTO update(Long roomId, Long roomEquipmentId, RoomEquipmentDTO dto) {
        LOG.debug("Request to update RoomEquipment {} for room {}", roomEquipmentId, roomId);
        requireAccessibleRoom(roomId);
        RoomEquipment existing = roomEquipmentRepository
            .findByIdAndRoomId(roomEquipmentId, roomId)
            .orElseThrow(() -> new BadRequestAlertException("Room equipment not found", ENTITY_NAME, "idnotfound"));
        if (dto.getQuantity() != null) {
            if (dto.getQuantity() < 0) {
                throw new BadRequestAlertException("Quantity cannot be negative", ENTITY_NAME, "invalidquantity");
            }
            existing.setQuantity(dto.getQuantity());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        return toDto(roomEquipmentRepository.save(existing));
    }

    public RoomEquipmentDTO reportBroken(Long roomId, Long roomEquipmentId) {
        LOG.debug("Request to report broken RoomEquipment {} for room {}", roomEquipmentId, roomId);
        requireAccessibleRoom(roomId);
        RoomEquipment existing = roomEquipmentRepository
            .findByIdAndRoomId(roomEquipmentId, roomId)
            .orElseThrow(() -> new BadRequestAlertException("Room equipment not found", ENTITY_NAME, "idnotfound"));
        existing.setStatus(RoomEquipmentStatus.BROKEN);
        return toDto(roomEquipmentRepository.save(existing));
    }

    public RoomEquipmentDTO toDto(RoomEquipment entity) {
        RoomEquipmentDTO dto = new RoomEquipmentDTO();
        dto.setId(entity.getId());
        dto.setRoomId(entity.getRoom() != null ? entity.getRoom().getId() : null);
        if (entity.getEquipment() != null) {
            dto.setEquipmentId(entity.getEquipment().getId());
            dto.setEquipmentName(entity.getEquipment().getName());
            dto.setCategory(entity.getEquipment().getCategory());
            dto.setUnitCost(entity.getEquipment().getUnitCost());
        }
        dto.setQuantity(entity.getQuantity());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private Room requireAccessibleRoom(Long roomId) {
        Room room = roomRepository
            .findById(roomId)
            .orElseThrow(() -> new BadRequestAlertException("Room not found", ENTITY_NAME, "roomnotfound"));
        User current = currentUserOrNull();
        if (!RoomAccessRules.canAccess(room, current)) {
            throw new BadRequestAlertException("Room not accessible", ENTITY_NAME, "roomforbidden");
        }
        return room;
    }

    private User currentUserOrNull() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).orElse(null);
    }
}
