package com.company.bookingroom.service;

import com.company.bookingroom.domain.Room;
import com.company.bookingroom.repository.BookingRepository;
import com.company.bookingroom.repository.RoomRepository;
import com.company.bookingroom.service.dto.RoomDTO;
import com.company.bookingroom.service.mapper.RoomMapper;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.company.bookingroom.domain.Room}.
 */
@Service
@Transactional
public class RoomService {

    private static final Logger LOG = LoggerFactory.getLogger(RoomService.class);
    private static final String ENTITY_NAME = "room";

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, RoomMapper roomMapper, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
        this.bookingRepository = bookingRepository;
    }

    public RoomDTO save(RoomDTO roomDTO) {
        LOG.debug("Request to save Room : {}", roomDTO);
        if (roomDTO.getIsActive() == null) {
            roomDTO.setIsActive(true);
        }
        Room room = roomMapper.toEntity(roomDTO);
        room = roomRepository.save(room);
        return roomMapper.toDto(room);
    }

    public RoomDTO update(RoomDTO roomDTO) {
        LOG.debug("Request to update Room : {}", roomDTO);
        if (Boolean.FALSE.equals(roomDTO.getIsActive())) {
            assertCanDeactivate(roomDTO.getId());
        }
        Room room = roomMapper.toEntity(roomDTO);
        room = roomRepository.save(room);
        return roomMapper.toDto(room);
    }

    public Optional<RoomDTO> partialUpdate(RoomDTO roomDTO) {
        LOG.debug("Request to partially update Room : {}", roomDTO);

        return roomRepository
            .findById(roomDTO.getId())
            .map(existingRoom -> {
                if (Boolean.FALSE.equals(roomDTO.getIsActive()) && Boolean.TRUE.equals(existingRoom.getIsActive())) {
                    assertCanDeactivate(existingRoom.getId());
                }
                roomMapper.partialUpdate(existingRoom, roomDTO);
                return existingRoom;
            })
            .map(roomRepository::save)
            .map(roomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RoomDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Rooms");
        return roomRepository.findAll(pageable).map(roomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<RoomDTO> findOne(Long id) {
        LOG.debug("Request to get Room : {}", id);
        return roomRepository.findById(id).map(roomMapper::toDto);
    }

    /**
     * Soft-delete: mark room inactive.
     */
    public void deactivate(Long id) {
        LOG.debug("Request to deactivate Room : {}", id);
        assertCanDeactivate(id);
        roomRepository
            .findById(id)
            .ifPresent(room -> {
                room.setIsActive(false);
                roomRepository.save(room);
            });
    }

    public void delete(Long id) {
        LOG.debug("Request to delete Room : {}", id);
        roomRepository.deleteById(id);
    }

    private void assertCanDeactivate(Long roomId) {
        if (bookingRepository.existsActiveBookingsForRoom(roomId, Instant.now())) {
            throw new BadRequestAlertException(
                "Không thể vô hiệu hóa phòng đang có lịch đặt (chờ duyệt hoặc đã duyệt chưa kết thúc)",
                ENTITY_NAME,
                "hasbookings"
            );
        }
    }
}
