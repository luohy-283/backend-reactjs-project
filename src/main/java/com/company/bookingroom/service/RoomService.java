package com.company.bookingroom.service;

import com.company.bookingroom.domain.Department;
import com.company.bookingroom.domain.Room;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.domain.enumeration.EquipmentCategory;
import com.company.bookingroom.domain.enumeration.RoomLayoutType;
import com.company.bookingroom.repository.BookingRepository;
import com.company.bookingroom.repository.DepartmentRepository;
import com.company.bookingroom.repository.RoomEquipmentRepository;
import com.company.bookingroom.repository.RoomRepository;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.SecurityUtils;
import com.company.bookingroom.service.dto.RoomDTO;
import com.company.bookingroom.service.mapper.RoomMapper;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final RoomEquipmentRepository roomEquipmentRepository;

    public RoomService(
        RoomRepository roomRepository,
        RoomMapper roomMapper,
        BookingRepository bookingRepository,
        UserRepository userRepository,
        DepartmentRepository departmentRepository,
        RoomEquipmentRepository roomEquipmentRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roomEquipmentRepository = roomEquipmentRepository;
    }

    public RoomDTO save(RoomDTO roomDTO) {
        LOG.debug("Request to save Room : {}", roomDTO);
        applyDefaults(roomDTO);
        Room room = roomMapper.toEntity(roomDTO);
        resolveLockedDepartment(room, roomDTO);
        room = roomRepository.save(room);
        return enrich(roomMapper.toDto(room));
    }

    public RoomDTO update(RoomDTO roomDTO) {
        LOG.debug("Request to update Room : {}", roomDTO);
        if (Boolean.FALSE.equals(roomDTO.getIsActive())) {
            assertCanDeactivate(roomDTO.getId());
        }
        applyDefaults(roomDTO);
        Room room = roomMapper.toEntity(roomDTO);
        resolveLockedDepartment(room, roomDTO);
        room = roomRepository.save(room);
        return enrich(roomMapper.toDto(room));
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
                // Form PATCH always sends `name` + `lockedDepartment` (object or JSON null).
                // Status-only PATCH sends only isActive — keep existing lock.
                if (roomDTO.getName() != null) {
                    resolveLockedDepartment(existingRoom, roomDTO);
                } else if (roomDTO.getLockedDepartment() != null && roomDTO.getLockedDepartment().getId() != null) {
                    resolveLockedDepartment(existingRoom, roomDTO);
                }
                return existingRoom;
            })
            .map(roomRepository::save)
            .map(roomMapper::toDto)
            .map(this::enrich);
    }

    /**
     * @param q optional text search (name, capacity, locked department name/code); blank = ignored
     * @param active {@code true}/{@code false} filter, or {@code null} for all (admin). Non-admin always sees active only.
     * @param vip {@code true}/{@code false} filter, or {@code null} for all
     * @param equipmentCategories optional AND filter: room must have OK inventory for every listed category
     */
    @Transactional(readOnly = true)
    public Page<RoomDTO> findAll(Pageable pageable, String q, Boolean active, Boolean vip, List<EquipmentCategory> equipmentCategories) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        Set<EquipmentCategory> categories = normalizeCategories(equipmentCategories);
        long categoryCount = categories.size();
        Collection<EquipmentCategory> categoryParam = categoryCount == 0
            ? List.of(EquipmentCategory.OTHER)
            : categories;
        LOG.debug(
            "Request to get Rooms (visibility filtered), q={}, active={}, vip={}, equipmentCategories={}",
            query,
            active,
            vip,
            categories
        );
        Page<RoomDTO> page;
        if (RoomAccessRules.isManagerOrAbove()) {
            page = roomRepository
                .findAllWithDepartment(active, vip, query, categoryParam, categoryCount, pageable)
                .map(roomMapper::toDto);
        } else {
            User current = requireCurrentUser();
            Long departmentId = current.getDepartment() != null ? current.getDepartment().getId() : null;
            page = roomRepository
                .findVisibleForDepartment(departmentId, true, vip, query, categoryParam, categoryCount, pageable)
                .map(roomMapper::toDto);
        }
        enrichPage(page.getContent());
        return page;
    }

    @Transactional(readOnly = true)
    public Optional<RoomDTO> findOne(Long id) {
        LOG.debug("Request to get Room : {}", id);
        return roomRepository
            .findById(id)
            .filter(room -> RoomAccessRules.canAccess(room, currentUserOrNull()))
            .map(roomMapper::toDto)
            .map(this::enrich);
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

    private void applyDefaults(RoomDTO roomDTO) {
        if (roomDTO.getIsActive() == null) {
            roomDTO.setIsActive(true);
        }
        if (roomDTO.getPricePerHour() == null) {
            roomDTO.setPricePerHour(BigDecimal.ZERO);
        }
        if (roomDTO.getIsVip() == null) {
            roomDTO.setIsVip(false);
        }
        if (roomDTO.getLayoutType() == null) {
            roomDTO.setLayoutType(RoomLayoutType.STANDARD);
        }
        if (roomDTO.getFloorWidthM() == null) {
            roomDTO.setFloorWidthM(new BigDecimal("6.00"));
        }
        if (roomDTO.getFloorDepthM() == null) {
            roomDTO.setFloorDepthM(new BigDecimal("4.50"));
        }
    }

    private Set<EquipmentCategory> normalizeCategories(List<EquipmentCategory> equipmentCategories) {
        if (equipmentCategories == null || equipmentCategories.isEmpty()) {
            return Set.of();
        }
        return equipmentCategories.stream().filter(c -> c != null).collect(Collectors.toCollection(() -> EnumSet.noneOf(EquipmentCategory.class)));
    }

    private RoomDTO enrich(RoomDTO dto) {
        if (dto.getId() != null) {
            enrichPage(List.of(dto));
        }
        return dto;
    }

    private void enrichPage(List<RoomDTO> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        List<Long> ids = rooms.stream().map(RoomDTO::getId).filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, Set<EquipmentCategory>> catsByRoom = new HashMap<>();
        Map<Long, Set<String>> namesByRoom = new HashMap<>();
        for (Object[] row : roomEquipmentRepository.findOkEquipmentByRoomIds(ids)) {
            Long roomId = (Long) row[0];
            EquipmentCategory category = (EquipmentCategory) row[1];
            String name = (String) row[2];
            catsByRoom.computeIfAbsent(roomId, k -> new LinkedHashSet<>()).add(category);
            if (name != null && !name.isBlank()) {
                namesByRoom.computeIfAbsent(roomId, k -> new LinkedHashSet<>()).add(name);
            }
        }
        for (RoomDTO room : rooms) {
            room.setEquipmentCategories(new ArrayList<>(catsByRoom.getOrDefault(room.getId(), Set.of())));
            room.setEquipmentNames(new ArrayList<>(namesByRoom.getOrDefault(room.getId(), Set.of())));
        }
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
