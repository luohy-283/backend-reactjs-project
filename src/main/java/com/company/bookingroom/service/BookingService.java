package com.company.bookingroom.service;

import com.company.bookingroom.domain.Booking;
import com.company.bookingroom.domain.Room;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.domain.enumeration.BookingStatus;
import com.company.bookingroom.repository.BookingRepository;
import com.company.bookingroom.repository.RoomRepository;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.security.SecurityUtils;
import com.company.bookingroom.service.dto.BookingDTO;
import com.company.bookingroom.service.mapper.BookingMapper;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.company.bookingroom.domain.Booking}.
 */
@Service
@Transactional
public class BookingService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY_NAME = "booking";

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    public BookingService(
        BookingRepository bookingRepository,
        BookingMapper bookingMapper,
        UserRepository userRepository,
        RoomRepository roomRepository,
        NotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }

    /**
     * Create a booking with overlap check and status by role.
     * USER → PENDING, ADMIN → APPROVED. user_id always taken from JWT.
     */
    public BookingDTO save(BookingDTO bookingDTO) {
        LOG.debug("Request to save Booking : {}", bookingDTO);
        validateTimeRange(bookingDTO);

        Long roomId = requireRoomId(bookingDTO);
        assertNoOverlap(roomId, bookingDTO.getStartTime(), bookingDTO.getEndTime(), null);

        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user not found in token", ENTITY_NAME, "usernotfound")
        );
        User currentUser = userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));

        Room room = roomRepository
            .findById(roomId)
            .orElseThrow(() -> new BadRequestAlertException("Room not found", ENTITY_NAME, "roomnotfound"));

        if (!RoomAccessRules.canAccess(room, currentUser)) {
            throw new BadRequestAlertException(
                "Bạn không có quyền đặt phòng này",
                ENTITY_NAME,
                "roomforbidden"
            );
        }

        BookingStatus status = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)
            ? BookingStatus.APPROVED
            : BookingStatus.PENDING;

        Booking booking = new Booking();
        booking.setTitle(bookingDTO.getTitle());
        booking.setStartTime(bookingDTO.getStartTime());
        booking.setEndTime(bookingDTO.getEndTime());
        booking.setStatus(status);
        booking.setRoom(room);
        booking.setUser(currentUser);

        BigDecimal pricePerHour = room.getPricePerHour() != null ? room.getPricePerHour() : BigDecimal.ZERO;
        booking.setPricePerHour(pricePerHour);
        booking.setAmount(BookingPricing.calculateAmount(pricePerHour, bookingDTO.getStartTime(), bookingDTO.getEndTime()));

        booking = bookingRepository.saveAndFlush(booking);
        Booking saved = bookingRepository
            .findOneWithEagerRelationships(booking.getId())
            .orElseThrow(() -> new BadRequestAlertException("Booking not found after save", ENTITY_NAME, "idnotfound"));
        if (status == BookingStatus.PENDING) {
            notificationService.notifyBookingPending(saved);
        }
        return bookingMapper.toDto(saved);
    }

    public BookingDTO update(BookingDTO bookingDTO) {
        LOG.debug("Request to update Booking : {}", bookingDTO);
        Booking booking = bookingMapper.toEntity(bookingDTO);
        booking = bookingRepository.save(booking);
        return bookingMapper.toDto(booking);
    }

    public Optional<BookingDTO> partialUpdate(BookingDTO bookingDTO) {
        LOG.debug("Request to partially update Booking : {}", bookingDTO);

        return bookingRepository
            .findById(bookingDTO.getId())
            .map(existingBooking -> {
                bookingMapper.partialUpdate(existingBooking, bookingDTO);
                return existingBooking;
            })
            .map(bookingRepository::save)
            .map(bookingMapper::toDto);
    }

    public Page<BookingDTO> findAll(
        Pageable pageable,
        LocalDate date,
        BookingStatus status,
        String q,
        Boolean upcoming
    ) {
        expirePastPending();
        boolean upcomingOnly = Boolean.TRUE.equals(upcoming);
        String search = (q == null || q.isBlank()) ? null : q.trim();
        LOG.debug(
            "Request to get Bookings pageable={}, date={}, status={}, q={}, upcoming={}",
            pageable,
            date,
            status,
            search,
            upcomingOnly
        );
        Pageable effectivePageable = withDefaultSort(pageable);
        boolean isAdmin = RoomAccessRules.isAdmin();
        Long departmentId = null;
        if (!isAdmin) {
            User current = requireCurrentUser();
            departmentId = current.getDepartment() != null ? current.getDepartment().getId() : null;
        }
        boolean hasDate = date != null;
        Instant dayStart = hasDate ? date.atStartOfDay(APP_ZONE).toInstant() : Instant.EPOCH;
        Instant dayEnd = hasDate ? date.plusDays(1).atStartOfDay(APP_ZONE).toInstant() : Instant.EPOCH;
        // Day filter without status → only active (PENDING/APPROVED), matching prior list behavior.
        boolean activeOnly = hasDate && status == null && !upcomingOnly;
        BookingStatus effectiveStatus = upcomingOnly && status == null ? BookingStatus.APPROVED : status;
        Page<Booking> page = bookingRepository.findVisibleFiltered(
            effectiveStatus,
            hasDate,
            dayStart,
            dayEnd,
            activeOnly,
            upcomingOnly,
            Instant.now(),
            search,
            isAdmin,
            departmentId,
            effectivePageable
        );
        return page.map(bookingMapper::toDto);
    }

    /**
     * Move PENDING bookings whose start time has passed to EXPIRED (history).
     * @return number of bookings expired
     */
    public int expirePastPending() {
        Instant now = Instant.now();
        List<Booking> stale = bookingRepository.findPendingStartingAtOrBefore(now);
        if (stale.isEmpty()) {
            return 0;
        }
        for (Booking booking : stale) {
            booking.setStatus(BookingStatus.EXPIRED);
            Booking saved = bookingRepository.save(booking);
            notificationService.clearPendingBookingNotifications(saved.getId());
            notificationService.notifyBookingExpired(saved);
        }
        LOG.info("Expired {} past PENDING booking(s)", stale.size());
        return stale.size();
    }

    private User requireCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user not found in token", ENTITY_NAME, "usernotfound")
        );
        return userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));
    }

    public Page<BookingDTO> findAllWithEagerRelationships(Pageable pageable) {
        return bookingRepository.findAllWithEagerRelationships(withDefaultSort(pageable)).map(bookingMapper::toDto);
    }

    private static Pageable withDefaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "startTime"));
    }

    @Transactional(readOnly = true)
    public Optional<BookingDTO> findOne(Long id) {
        LOG.debug("Request to get Booking : {}", id);
        return bookingRepository.findOneWithEagerRelationships(id).map(bookingMapper::toDto);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete Booking : {}", id);
        bookingRepository.deleteById(id);
    }

    public BookingDTO approve(Long id) {
        expirePastPending();
        Booking booking = bookingRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestAlertException("Only PENDING bookings can be approved", ENTITY_NAME, "invalidstatus");
        }

        assertNoOverlap(booking.getRoom().getId(), booking.getStartTime(), booking.getEndTime(), booking.getId());
        booking.setStatus(BookingStatus.APPROVED);
        Booking saved = bookingRepository.save(booking);
        notificationService.clearPendingBookingNotifications(saved.getId());
        notificationService.notifyBookingApproved(saved);
        return bookingMapper.toDto(saved);
    }

    public BookingDTO reject(Long id) {
        expirePastPending();
        Booking booking = bookingRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestAlertException("Only PENDING bookings can be rejected", ENTITY_NAME, "invalidstatus");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        notificationService.clearPendingBookingNotifications(saved.getId());
        notificationService.notifyBookingRejected(saved);
        return bookingMapper.toDto(saved);
    }

    /**
     * Cancel an APPROVED booking that has not started yet.
     */
    public BookingDTO cancel(Long id) {
        Booking booking = bookingRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestAlertException("Only APPROVED bookings can be cancelled", ENTITY_NAME, "invalidstatus");
        }
        if (!booking.getStartTime().isAfter(Instant.now())) {
            throw new BadRequestAlertException(
                "Không thể hủy lịch đã bắt đầu hoặc đã qua",
                ENTITY_NAME,
                "alreadystarted"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        notificationService.notifyBookingCancelled(saved);
        return bookingMapper.toDto(saved);
    }

    private void validateTimeRange(BookingDTO bookingDTO) {
        if (bookingDTO.getStartTime() == null || bookingDTO.getEndTime() == null) {
            throw new BadRequestAlertException("startTime and endTime are required", ENTITY_NAME, "timerange");
        }
        if (!bookingDTO.getEndTime().isAfter(bookingDTO.getStartTime())) {
            throw new BadRequestAlertException("endTime must be after startTime", ENTITY_NAME, "timerange");
        }
    }

    private Long requireRoomId(BookingDTO bookingDTO) {
        if (bookingDTO.getRoom() == null || bookingDTO.getRoom().getId() == null) {
            throw new BadRequestAlertException("room id is required", ENTITY_NAME, "roomrequired");
        }
        return bookingDTO.getRoom().getId();
    }

    private void assertNoOverlap(Long roomId, Instant startTime, Instant endTime, Long excludeId) {
        if (bookingRepository.existsOverlapping(roomId, startTime, endTime, excludeId)) {
            throw new BadRequestAlertException(
                "Phòng đã được đặt trong khoảng thời gian này",
                ENTITY_NAME,
                "overlap"
            );
        }
    }
}
