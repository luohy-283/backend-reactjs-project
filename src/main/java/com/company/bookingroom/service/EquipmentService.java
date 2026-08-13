package com.company.bookingroom.service;

import com.company.bookingroom.domain.Equipment;
import com.company.bookingroom.repository.EquipmentRepository;
import com.company.bookingroom.service.dto.EquipmentDTO;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EquipmentService {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentService.class);
    private static final String ENTITY_NAME = "equipment";

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public EquipmentDTO save(EquipmentDTO dto) {
        LOG.debug("Request to save Equipment : {}", dto);
        if (dto.getIsActive() == null) {
            dto.setIsActive(true);
        }
        if (dto.getUnitCost() == null) {
            dto.setUnitCost(BigDecimal.ZERO);
        }
        Equipment entity = toEntity(dto);
        entity = equipmentRepository.save(entity);
        return toDto(entity);
    }

    public EquipmentDTO update(EquipmentDTO dto) {
        LOG.debug("Request to update Equipment : {}", dto);
        Equipment entity = toEntity(dto);
        entity = equipmentRepository.save(entity);
        return toDto(entity);
    }

    public Optional<EquipmentDTO> partialUpdate(EquipmentDTO dto) {
        LOG.debug("Request to partially update Equipment : {}", dto);
        return equipmentRepository
            .findById(dto.getId())
            .map(existing -> {
                if (dto.getName() != null) {
                    existing.setName(dto.getName());
                }
                if (dto.getCategory() != null) {
                    existing.setCategory(dto.getCategory());
                }
                if (dto.getUnitCost() != null) {
                    existing.setUnitCost(dto.getUnitCost());
                }
                if (dto.getIsActive() != null) {
                    existing.setIsActive(dto.getIsActive());
                }
                return existing;
            })
            .map(equipmentRepository::save)
            .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<EquipmentDTO> findAll(Pageable pageable, String q, Boolean active) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return equipmentRepository.findAllFiltered(active, query, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<EquipmentDTO> findOne(Long id) {
        return equipmentRepository.findById(id).map(this::toDto);
    }

    public Equipment require(Long id) {
        return equipmentRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Equipment not found", ENTITY_NAME, "idnotfound"));
    }

    public EquipmentDTO toDto(Equipment entity) {
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCategory(entity.getCategory());
        dto.setUnitCost(entity.getUnitCost());
        dto.setIsActive(entity.getIsActive());
        return dto;
    }

    private Equipment toEntity(EquipmentDTO dto) {
        Equipment entity = new Equipment();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setUnitCost(dto.getUnitCost());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return entity;
    }
}
