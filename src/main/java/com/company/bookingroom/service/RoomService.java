package com.company.bookingroom.service;

import com.company.bookingroom.domain.Department;
import com.company.bookingroom.domain.Room;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.repository.BookingRepository;
import com.company.bookingroom.repository.DepartmentRepository;
import com.company.bookingroom.repository.RoomRepository;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.SecurityUtils;
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
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public RoomService(
        RoomRepository roomRepository,
        RoomMapper roomMapper,
        BookingRepository bookingRepository,
        UserRepository userRepository,
        DepartmentRepository departmentRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    public RoomDTO save(RoomDTO roomDTO) {
        LOG.debug("Request to save Room : {}", roomDTO);
        if (roomDTO.getIsActive() == null) {
            roomDTO.setIsActive(true);
        }
        if (roomDTO.getPricePerHour() == null) {
            roomDTO.setPricePerHour(java.math.BigDecimal.ZERO);
        }
        Room room = roomMapper.toEntity(roomDTO);
        resolveLockedDepartment(room, roomDTO);
        room = roomRepository.save(room);
        return roomMapper.toDto(room);
    }

    public RoomDTO update(RoomDTO roomDTO) {
        LOG.debug("Request to update Room : {}", roomDTO);
        if (Boolean.FALSE.equals(roomDTO.getIsActive())) {
            assertCanDeactivate(roomDTO.getId());
        }
        Room room = roomMapper.toEntity(roomDTO);
        resolveLockedDepartment(room, roomDTO);
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
                if (roomDTO.getLockedDepartment() != null && roomDTO.getLockedDepartment().getId() != null) {
                    resolveLockedDepartment(existingRoom, roomDTO);
                }
                return existingRoom;
            })
            .map(roomRepository::save)
            .map(roomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RoomDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Rooms (visibility filtered)");
        if (RoomAccessRules.isAdmin()) {
            return roomRepository.findAllWithDepartment(false, pageable).map(roomMapper::toDto);
        }
        User current = requireCurrentUser();
        Long departmentId = current.getDepartment() != null ? current.getDepartment().getId() : null;
        return roomRepository.findVisibleForDepartment(departmentId, true, pageable).map(roomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<RoomDTO> findOne(Long id) {
        LOG.debug("Request to get Room : {}", id);
        return roomRepository
            .findById(id)
            .filter(room -> RoomAccessRules.canAccess(room, currentUserOrNull()))
            .map(roomMapper::toDto);
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

    private void resolveLockedDepartment(Room room, RoomDTO roomDTO) {
        if (roomDTO.getLockedDepartment() == null || roomDTO.getLockedDepartment().getId() == null) {
            room.setLockedDepartment(null);
            return;
        }
        Department department = departmentRepository
            .findById(roomDTO.getLockedDepartment().getId())
            .orElseThrow(() -> new BadRequestAlertException("Department not found", ENTITY_NAME, "departmentnotfound"));
        room.setLockedDepartment(department);
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

    private User requireCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user not found in token", ENTITY_NAME, "usernotfound")
        );
        return userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));
    }

    private User currentUserOrNull() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).orElse(null);
    }
}
