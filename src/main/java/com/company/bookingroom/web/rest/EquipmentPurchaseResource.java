package com.company.bookingroom.web.rest;

import com.company.bookingroom.domain.enumeration.PurchaseStatus;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.EquipmentPurchaseService;
import com.company.bookingroom.service.dto.EquipmentPurchaseDTO;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
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
@RequestMapping("/api/equipment-purchases")
public class EquipmentPurchaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentPurchaseResource.class);
    private static final String ENTITY_NAME = "equipmentPurchase";

    @Value("${jhipster.clientApp.name:backendbookmeetingroom}")
    private String applicationName;

    private final EquipmentPurchaseService equipmentPurchaseService;

    public EquipmentPurchaseResource(EquipmentPurchaseService equipmentPurchaseService) {
        this.equipmentPurchaseService = equipmentPurchaseService;
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public ResponseEntity<EquipmentPurchaseDTO> create(@Valid @RequestBody EquipmentPurchaseDTO dto) throws URISyntaxException {
        LOG.debug("REST request to create EquipmentPurchase : {}", dto);
        EquipmentPurchaseDTO result = equipmentPurchaseService.create(dto);
        return ResponseEntity.created(new URI("/api/equipment-purchases/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @GetMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public Page<EquipmentPurchaseDTO> getAll(
        @RequestParam(required = false) PurchaseStatus status,
        @RequestParam(required = false) Long roomId,
        @PageableDefault(size = 20) @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get EquipmentPurchases status={}, roomId={}", status, roomId);
        return equipmentPurchaseService.findAll(status, roomId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public ResponseEntity<EquipmentPurchaseDTO> getOne(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(equipmentPurchaseService.findOne(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.MANAGER + "\")")
    public ResponseEntity<EquipmentPurchaseDTO> approve(@PathVariable Long id) {
        LOG.debug("REST request to approve EquipmentPurchase : {}", id);
        return ResponseEntity.ok(equipmentPurchaseService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.MANAGER + "\")")
    public ResponseEntity<EquipmentPurchaseDTO> reject(@PathVariable Long id) {
        LOG.debug("REST request to reject EquipmentPurchase : {}", id);
        return ResponseEntity.ok(equipmentPurchaseService.reject(id));
    }

    @PostMapping("/{id}/fulfill")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public ResponseEntity<EquipmentPurchaseDTO> fulfill(@PathVariable Long id) {
        LOG.debug("REST request to fulfill EquipmentPurchase : {}", id);
        return ResponseEntity.ok(equipmentPurchaseService.fulfill(id));
    }
}
