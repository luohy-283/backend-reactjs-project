package com.company.bookingroom.service;

import com.company.bookingroom.domain.Booking;
import com.company.bookingroom.repository.BookingRepository;
import com.company.bookingroom.security.SecurityUtils;
import com.company.bookingroom.service.dto.BookingDTO;
import com.company.bookingroom.service.dto.RevenueByDayDTO;
import com.company.bookingroom.service.dto.RevenueByRoomDTO;
import com.company.bookingroom.service.dto.RevenuePeriodDTO;
import com.company.bookingroom.service.dto.RevenueReportDTO;
import com.company.bookingroom.service.mapper.BookingMapper;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvoiceRevenueService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY_NAME = "invoice";

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    public InvoiceRevenueService(BookingRepository bookingRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional(readOnly = true)
    public Page<BookingDTO> findMyInvoices(Pageable pageable) {
        String login = requireLogin();
        return bookingRepository.findApprovedInvoicesByLogin(login, pageable).map(bookingMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<BookingDTO> findMyInvoice(Long id) {
        String login = requireLogin();
        return bookingRepository.findApprovedInvoiceByIdAndLogin(id, login).map(bookingMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RevenueReportDTO getMonthlyRevenue(String yearMonth) {
        YearMonth ym = parseYearMonth(yearMonth);
        YearMonth previousYm = ym.minusMonths(1);

        Instant monthStart = ym.atDay(1).atStartOfDay(APP_ZONE).toInstant();
        Instant monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay(APP_ZONE).toInstant();
        Instant prevStart = previousYm.atDay(1).atStartOfDay(APP_ZONE).toInstant();
        Instant prevEnd = ym.atDay(1).atStartOfDay(APP_ZONE).toInstant();

        List<Booking> bookings = bookingRepository.findApprovedInMonth(monthStart, monthEnd);
        long cancelledCount = bookingRepository.countCancelledInMonth(monthStart, monthEnd);

        List<Booking> previousBookings = bookingRepository.findApprovedInMonth(prevStart, prevEnd);
        long previousCancelled = bookingRepository.countCancelledInMonth(prevStart, prevEnd);

        RevenuePeriodDTO currentPeriod = buildPeriod(ym, bookings, cancelledCount);
        RevenuePeriodDTO previousPeriod = buildPeriod(previousYm, previousBookings, previousCancelled);

        BigDecimal total = currentPeriod.getTotalAmount();
        Map<Long, RevenueByRoomDTO> byRoom = new LinkedHashMap<>();
        Map<LocalDate, RevenueByDayDTO> byDay = new LinkedHashMap<>();

        for (LocalDate day = ym.atDay(1); !day.isAfter(ym.atEndOfMonth()); day = day.plusDays(1)) {
            RevenueByDayDTO empty = new RevenueByDayDTO();
            empty.setDate(day.toString());
            empty.setBookingCount(0);
            empty.setAmount(BigDecimal.ZERO);
            byDay.put(day, empty);
        }

        for (Booking booking : bookings) {
            BigDecimal amount = booking.getAmount() != null ? booking.getAmount() : BigDecimal.ZERO;

            Long roomId = booking.getRoom().getId();
            RevenueByRoomDTO roomRow = byRoom.computeIfAbsent(roomId, id -> {
                RevenueByRoomDTO dto = new RevenueByRoomDTO();
                dto.setRoomId(id);
                dto.setRoomName(booking.getRoom().getName());
                dto.setBookingCount(0);
                dto.setAmount(BigDecimal.ZERO);
                dto.setSharePercent(BigDecimal.ZERO);
                return dto;
            });
            roomRow.setBookingCount(roomRow.getBookingCount() + 1);
            roomRow.setAmount(roomRow.getAmount().add(amount));

            LocalDate day = booking.getStartTime().atZone(APP_ZONE).toLocalDate();
            RevenueByDayDTO dayRow = byDay.get(day);
            if (dayRow != null) {
                dayRow.setBookingCount(dayRow.getBookingCount() + 1);
                dayRow.setAmount(dayRow.getAmount().add(amount));
            }
        }

        List<RevenueByRoomDTO> roomList = new ArrayList<>(byRoom.values());
        roomList.sort(Comparator.comparing(RevenueByRoomDTO::getAmount).reversed());
        for (RevenueByRoomDTO roomRow : roomList) {
            roomRow.setSharePercent(sharePercent(roomRow.getAmount(), total));
        }

        RevenueReportDTO report = new RevenueReportDTO();
        report.setYearMonth(currentPeriod.getYearMonth());
        report.setTotalAmount(currentPeriod.getTotalAmount());
        report.setTotalBookings(currentPeriod.getTotalBookings());
        report.setAverageAmount(currentPeriod.getAverageAmount());
        report.setCancelledCount(currentPeriod.getCancelledCount());
        report.setCancellationRate(currentPeriod.getCancellationRate());
        report.setPrevious(previousPeriod);
        report.setByRoom(roomList);
        report.setByDay(new ArrayList<>(byDay.values()));
        return report;
    }

    private static RevenuePeriodDTO buildPeriod(YearMonth ym, List<Booking> approved, long cancelledCount) {
        BigDecimal total = BigDecimal.ZERO;
        for (Booking booking : approved) {
            total = total.add(booking.getAmount() != null ? booking.getAmount() : BigDecimal.ZERO);
        }
        long bookings = approved.size();
        long attempts = bookings + cancelledCount;

        RevenuePeriodDTO period = new RevenuePeriodDTO();
        period.setYearMonth(ym.toString());
        period.setTotalAmount(total);
        period.setTotalBookings(bookings);
        period.setAverageAmount(
            bookings == 0
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(bookings), 2, RoundingMode.HALF_UP)
        );
        period.setCancelledCount(cancelledCount);
        period.setCancellationRate(
            attempts == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(cancelledCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(attempts), 2, RoundingMode.HALF_UP)
        );
        return period;
    }

    private static BigDecimal sharePercent(BigDecimal amount, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static YearMonth parseYearMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.now(APP_ZONE);
        }
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException ex) {
            throw new BadRequestAlertException("yearMonth must be yyyy-MM", "revenue", "invalidmonth");
        }
    }

    private String requireLogin() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user not found in token", ENTITY_NAME, "usernotfound")
        );
    }
}
