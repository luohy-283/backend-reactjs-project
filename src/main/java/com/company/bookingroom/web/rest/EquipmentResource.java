package com.company.bookingroom.web.rest;

import com.company.bookingroom.repository.EquipmentRepository;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.EquipmentService;
import com.company.bookingroom.service.dto.EquipmentDTO;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentResource.class);
    private static final String ENTITY_NAME = "equipment";

    @Value("${jhipster.clientApp.name:backendbookmeetingroom}")
    private String applicationName;

    private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;

    public EquipmentResource(EquipmentService equipmentService, EquipmentRepository equipmentRepository) {
        this.equipmentService = equipmentService;
        this.equipmentRepository = equipmentRepository;
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public ResponseEntity<EquipmentDTO> create(@Valid @RequestBody EquipmentDTO dto) throws URISyntaxException {
        LOG.debug("REST request to save Equipment : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new equipment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        dto = equipmentService.save(dto);
        return ResponseEntity.created(new URI("/api/equipment/" + dto.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, dto.getId().toString()))
            .body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public ResponseEntity<EquipmentDTO> update(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody EquipmentDTO dto
    ) {
        LOG.debug("REST request to update Equipment : {}, {}", id, dto);
        bindPathId(id, dto);
        if (!equipmentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        dto = equipmentService.update(dto);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, dto.getId().toString()))
            .body(dto);
    }

    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public ResponseEntity<EquipmentDTO> partialUpdate(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody EquipmentDTO dto
    ) {
        LOG.debug("REST request to partial update Equipment : {}, {}", id, dto);
        bindPathId(id, dto);
        if (!equipmentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<EquipmentDTO> result = equipmentService.partialUpdate(dto);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, dto.getId().toString())
        );
    }

    @GetMapping("")
    public Page<EquipmentDTO> getAll(
        @PageableDefault(size = 20) @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "q", required = false) String q,
        @RequestParam(name = "active", required = false) Boolean active
    ) {
        LOG.debug("REST request to get Equipment page, q={}, active={}", q, active);
        return equipmentService.findAll(pageable, q, active);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentDTO> getOne(@PathVariable("id") Long id) {
        return ResponseUtil.wrapOrNotFound(equipmentService.findOne(id));
    }

    private static void bindPathId(Long pathId, EquipmentDTO dto) {
        if (pathId == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (dto.getId() == null) {
            dto.setId(pathId);
            return;
        }
        if (!Objects.equals(pathId, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
    }
}
