package com.company.bookingroom.service;

import com.company.bookingroom.domain.Equipment;
import com.company.bookingroom.domain.EquipmentPurchase;
import com.company.bookingroom.domain.Room;
import com.company.bookingroom.domain.RoomEquipment;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.domain.enumeration.PurchaseStatus;
import com.company.bookingroom.domain.enumeration.RoomEquipmentStatus;
import com.company.bookingroom.repository.EquipmentPurchaseRepository;
import com.company.bookingroom.repository.RoomEquipmentRepository;
import com.company.bookingroom.repository.RoomRepository;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.SecurityUtils;
import com.company.bookingroom.service.dto.EquipmentPurchaseDTO;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EquipmentPurchaseService {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentPurchaseService.class);
    private static final String ENTITY_NAME = "equipmentPurchase";

    private final EquipmentPurchaseRepository equipmentPurchaseRepository;
    private final RoomRepository roomRepository;
    private final RoomEquipmentRepository roomEquipmentRepository;
    private final EquipmentService equipmentService;
    private final UserRepository userRepository;

    public EquipmentPurchaseService(
        EquipmentPurchaseRepository equipmentPurchaseRepository,
        RoomRepository roomRepository,
        RoomEquipmentRepository roomEquipmentRepository,
        EquipmentService equipmentService,
        UserRepository userRepository
    ) {
        this.equipmentPurchaseRepository = equipmentPurchaseRepository;
        this.roomRepository = roomRepository;
        this.roomEquipmentRepository = roomEquipmentRepository;
        this.equipmentService = equipmentService;
        this.userRepository = userRepository;
    }

    public EquipmentPurchaseDTO create(EquipmentPurchaseDTO dto) {
        LOG.debug("Request to create EquipmentPurchase : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new purchase cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Room room = roomRepository
            .findById(dto.getRoomId())
            .orElseThrow(() -> new BadRequestAlertException("Room not found", ENTITY_NAME, "roomnotfound"));
        Equipment equipment = equipmentService.require(dto.getEquipmentId());
        if (Boolean.FALSE.equals(equipment.getIsActive())) {
            throw new BadRequestAlertException("Equipment is inactive", ENTITY_NAME, "equipmentinactive");
        }
        User requester = requireCurrentUser();

        EquipmentPurchase purchase = new EquipmentPurchase();
        purchase.setRoom(room);
        purchase.setEquipment(equipment);
        purchase.setQuantity(dto.getQuantity());
        purchase.setUnitCost(equipment.getUnitCost() != null ? equipment.getUnitCost() : BigDecimal.ZERO);
        purchase.setReason(dto.getReason());
        purchase.setStatus(PurchaseStatus.PENDING);
        purchase.setRequestedBy(requester);
        purchase = equipmentPurchaseRepository.save(purchase);
        return toDto(purchase);
    }

    @Transactional(readOnly = true)
    public Page<EquipmentPurchaseDTO> findAll(PurchaseStatus status, Long roomId, Pageable pageable) {
        return equipmentPurchaseRepository.findFiltered(status, roomId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<EquipmentPurchaseDTO> findOne(Long id) {
        return equipmentPurchaseRepository.findById(id).map(this::toDto);
    }

    public EquipmentPurchaseDTO approve(Long id) {
        EquipmentPurchase purchase = require(id);
        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            throw new BadRequestAlertException("Only PENDING purchases can be approved", ENTITY_NAME, "invalidstatus");
        }
        purchase.setStatus(PurchaseStatus.APPROVED);
        purchase.setApprovedBy(requireCurrentUser());
        return toDto(equipmentPurchaseRepository.save(purchase));
    }

    public EquipmentPurchaseDTO reject(Long id) {
        EquipmentPurchase purchase = require(id);
        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            throw new BadRequestAlertException("Only PENDING purchases can be rejected", ENTITY_NAME, "invalidstatus");
        }
        purchase.setStatus(PurchaseStatus.REJECTED);
        purchase.setApprovedBy(requireCurrentUser());
        return toDto(equipmentPurchaseRepository.save(purchase));
    }

    public EquipmentPurchaseDTO fulfill(Long id) {
        EquipmentPurchase purchase = require(id);
        if (purchase.getStatus() != PurchaseStatus.APPROVED) {
            throw new BadRequestAlertException("Only APPROVED purchases can be fulfilled", ENTITY_NAME, "invalidstatus");
        }
        Instant now = Instant.now();
        purchase.setStatus(PurchaseStatus.FULFILLED);
        purchase.setFulfilledAt(now);

        Long roomId = purchase.getRoom().getId();
        Long equipmentId = purchase.getEquipment().getId();
        RoomEquipment inventory = roomEquipmentRepository
            .findByRoomIdAndEquipmentId(roomId, equipmentId)
            .orElseGet(() -> {
                RoomEquipment created = new RoomEquipment();
                created.setRoom(purchase.getRoom());
                created.setEquipment(purchase.getEquipment());
                created.setQuantity(0);
                created.setStatus(RoomEquipmentStatus.OK);
                return created;
            });
        int qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        inventory.setQuantity(qty + purchase.getQuantity());
        inventory.setStatus(RoomEquipmentStatus.OK);
        roomEquipmentRepository.save(inventory);

        return toDto(equipmentPurchaseRepository.save(purchase));
    }

    private EquipmentPurchase require(Long id) {
        return equipmentPurchaseRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Purchase not found", ENTITY_NAME, "idnotfound"));
    }

    private User requireCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user not found in token", ENTITY_NAME, "usernotfound")
        );
        return userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));
    }

    public EquipmentPurchaseDTO toDto(EquipmentPurchase entity) {
        EquipmentPurchaseDTO dto = new EquipmentPurchaseDTO();
        dto.setId(entity.getId());
        if (entity.getRoom() != null) {
            dto.setRoomId(entity.getRoom().getId());
            dto.setRoomName(entity.getRoom().getName());
        }
        if (entity.getEquipment() != null) {
            dto.setEquipmentId(entity.getEquipment().getId());
            dto.setEquipmentName(entity.getEquipment().getName());
        }
        dto.setQuantity(entity.getQuantity());
        dto.setUnitCost(entity.getUnitCost());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        if (entity.getRequestedBy() != null) {
            dto.setRequestedById(entity.getRequestedBy().getId());
            dto.setRequestedByLogin(entity.getRequestedBy().getLogin());
        }
        if (entity.getApprovedBy() != null) {
            dto.setApprovedById(entity.getApprovedBy().getId());
            dto.setApprovedByLogin(entity.getApprovedBy().getLogin());
        }
        dto.setFulfilledAt(entity.getFulfilledAt());
        dto.setCreatedDate(entity.getCreatedDate());
        return dto;
    }
}
