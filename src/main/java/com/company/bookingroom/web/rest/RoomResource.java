package com.company.bookingroom.web.rest;

import com.company.bookingroom.repository.RoomRepository;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.RoomService;
import com.company.bookingroom.service.dto.RoomDTO;
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

/**
 * REST controller for managing {@link com.company.bookingroom.domain.Room}.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoomResource.class);

    private static final String ENTITY_NAME = "room";

    @Value("${jhipster.clientApp.name:backendbookmeetingroom}")
    private String applicationName;

    private final RoomService roomService;

    private final RoomRepository roomRepository;

    public RoomResource(RoomService roomService, RoomRepository roomRepository) {
        this.roomService = roomService;
        this.roomRepository = roomRepository;
    }

    /**
     * {@code POST  /rooms} : Create a new room.
     *
     * @param roomDTO the roomDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new roomDTO, or with status {@code 400 (Bad Request)} if the room has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomDTO roomDTO) throws URISyntaxException {
        LOG.debug("REST request to save Room : {}", roomDTO);
        if (roomDTO.getId() != null) {
            throw new BadRequestAlertException("A new room cannot already have an ID", ENTITY_NAME, "idexists");
        }
        roomDTO = roomService.save(roomDTO);
        return ResponseEntity.created(new URI("/api/rooms/" + roomDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, roomDTO.getId().toString()))
            .body(roomDTO);
    }

    /**
     * {@code PUT  /rooms/:id} : Updates an existing room.
     *
     * @param id the id of the roomDTO to save.
     * @param roomDTO the roomDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomDTO,
     * or with status {@code 400 (Bad Request)} if the roomDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the roomDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<RoomDTO> updateRoom(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RoomDTO roomDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Room : {}, {}", id, roomDTO);
        if (roomDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        roomDTO = roomService.update(roomDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, roomDTO.getId().toString()))
            .body(roomDTO);
    }

    /**
     * {@code PATCH  /rooms/:id} : Partial updates given fields of an existing room, field will ignore if it is null
     *
     * @param id the id of the roomDTO to save.
     * @param roomDTO the roomDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomDTO,
     * or with status {@code 400 (Bad Request)} if the roomDTO is not valid,
     * or with status {@code 404 (Not Found)} if the roomDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the roomDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<RoomDTO> partialUpdateRoom(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RoomDTO roomDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Room partially : {}, {}", id, roomDTO);
        if (roomDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RoomDTO> result = roomService.partialUpdate(roomDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, roomDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /api/rooms} : get a page of Rooms.
     * Query: {@code page} (0-based), {@code size}, {@code sort} — e.g. {@code /api/rooms?page=0&size=10&sort=id,asc}
     */
    @GetMapping("")
    public Page<RoomDTO> getAllRooms(
        @PageableDefault(size = 20) @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of Rooms");
        return roomService.findAll(pageable);
    }

    /**
     * {@code GET  /rooms/:id} : get the "id" room.
     *
     * @param id the id of the roomDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the roomDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Room : {}", id);
        Optional<RoomDTO> roomDTO = roomService.findOne(id);
        return ResponseUtil.wrapOrNotFound(roomDTO);
    }

    /**
     * Soft-delete: set isActive = false instead of hard delete.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteRoom(@PathVariable("id") Long id) {
        LOG.debug("REST request to soft-delete Room : {}", id);
        roomService.deactivate(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
