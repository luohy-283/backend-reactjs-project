package com.company.bookingroom.web.rest;

import com.company.bookingroom.domain.enumeration.BookingStatus;
import com.company.bookingroom.repository.BookingRepository;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.BookingService;
import com.company.bookingroom.service.dto.BookingDTO;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.company.bookingroom.domain.Booking}.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingResource {

    private static final Logger LOG = LoggerFactory.getLogger(BookingResource.class);

    private static final String ENTITY_NAME = "booking";

    @Value("${jhipster.clientApp.name:backendbookmeetingroom}")
    private String applicationName;

    private final BookingService bookingService;

    private final BookingRepository bookingRepository;

    public BookingResource(BookingService bookingService, BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    /**
     * {@code POST  /bookings} : Create a new booking.
     *
     * @param bookingDTO the bookingDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new bookingDTO, or with status {@code 400 (Bad Request)} if the booking has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<BookingDTO> createBooking(@Valid @RequestBody BookingDTO bookingDTO) throws URISyntaxException {
        LOG.debug("REST request to save Booking : {}", bookingDTO);
        if (bookingDTO.getId() != null) {
            throw new BadRequestAlertException("A new booking cannot already have an ID", ENTITY_NAME, "idexists");
        }
        bookingDTO = bookingService.save(bookingDTO);
        return ResponseEntity.created(new URI("/api/bookings/" + bookingDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, bookingDTO.getId().toString()))
            .body(bookingDTO);
    }

    /**
     * {@code PUT  /bookings/:id} : Updates an existing booking.
     *
     * @param id the id of the bookingDTO to save.
     * @param bookingDTO the bookingDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated bookingDTO,
     * or with status {@code 400 (Bad Request)} if the bookingDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the bookingDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookingDTO> updateBooking(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody BookingDTO bookingDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Booking : {}, {}", id, bookingDTO);
        if (bookingDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, bookingDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!bookingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        bookingDTO = bookingService.update(bookingDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, bookingDTO.getId().toString()))
            .body(bookingDTO);
    }

    /**
     * {@code PATCH  /bookings/:id} : Partial updates given fields of an existing booking, field will ignore if it is null
     *
     * @param id the id of the bookingDTO to save.
     * @param bookingDTO the bookingDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated bookingDTO,
     * or with status {@code 400 (Bad Request)} if the bookingDTO is not valid,
     * or with status {@code 404 (Not Found)} if the bookingDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the bookingDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<BookingDTO> partialUpdateBooking(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody BookingDTO bookingDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Booking partially : {}, {}", id, bookingDTO);
        if (bookingDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, bookingDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!bookingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<BookingDTO> result = bookingService.partialUpdate(bookingDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, bookingDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /api/bookings} : get a page of bookings.
     * Query: {@code page}, {@code size}, {@code sort} (default {@code startTime,desc}), optional {@code date}, {@code status}.
     * Example: {@code /api/bookings?page=0&size=10&sort=startTime,desc}
     */
    @GetMapping("")
    public Page<BookingDTO> getAllBookings(
        @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC)
        @org.springdoc.core.annotations.ParameterObject
        Pageable pageable,
        @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(name = "status", required = false) BookingStatus status
    ) {
        LOG.debug("REST request to get a page of Bookings, date={}, status={}", date, status);
        return bookingService.findAll(pageable, date, status);
    }

    /**
     * {@code POST  /bookings/:id/approve} : approve a PENDING booking (ADMIN).
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookingDTO> approveBooking(@PathVariable("id") Long id) {
        LOG.debug("REST request to approve Booking : {}", id);
        BookingDTO result = bookingService.approve(id);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code POST  /bookings/:id/reject} : reject a PENDING booking (ADMIN).
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookingDTO> rejectBooking(@PathVariable("id") Long id) {
        LOG.debug("REST request to reject Booking : {}", id);
        BookingDTO result = bookingService.reject(id);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code POST  /bookings/:id/cancel} : cancel an APPROVED booking that has not started (ADMIN).
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookingDTO> cancelBooking(@PathVariable("id") Long id) {
        LOG.debug("REST request to cancel Booking : {}", id);
        BookingDTO result = bookingService.cancel(id);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code GET  /bookings/:id} : get the "id" booking.
     *
     * @param id the id of the bookingDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the bookingDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBooking(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Booking : {}", id);
        Optional<BookingDTO> bookingDTO = bookingService.findOne(id);
        return ResponseUtil.wrapOrNotFound(bookingDTO);
    }

    /**
     * {@code DELETE  /bookings/:id} : delete the "id" booking.
     *
     * @param id the id of the bookingDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteBooking(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Booking : {}", id);
        bookingService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
