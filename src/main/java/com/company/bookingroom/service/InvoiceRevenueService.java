package com.company.bookingroom.service;

import com.company.bookingroom.domain.Booking;
import com.company.bookingroom.domain.EquipmentPurchase;
import com.company.bookingroom.repository.BookingRepository;
import com.company.bookingroom.repository.EquipmentPurchaseRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvoiceRevenueService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY_NAME = "invoice";

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final EquipmentPurchaseRepository equipmentPurchaseRepository;

    public InvoiceRevenueService(
        BookingRepository bookingRepository,
        BookingMapper bookingMapper,
        EquipmentPurchaseRepository equipmentPurchaseRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.equipmentPurchaseRepository = equipmentPurchaseRepository;
    }

    @Transactional(readOnly = true)
    public Page<BookingDTO> findMyInvoices(String q, com.company.bookingroom.domain.enumeration.PaymentStatus paymentStatus, Pageable pageable) {
        String login = requireLogin();
        String needle = normalizeSearch(q);
        return bookingRepository.findApprovedInvoicesByLogin(login, needle, paymentStatus, pageable).map(bookingMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<BookingDTO> findMyInvoice(Long id) {
        String login = requireLogin();
        return bookingRepository.findApprovedInvoiceByIdAndLogin(id, login).map(bookingMapper::toDto);
    }

    @Transactional(readOnly = true)
    public byte[] exportMyInvoicesCsv() {
        String login = requireLogin();
        List<Booking> bookings = bookingRepository
            .findApprovedInvoicesByLogin(login, null, null, Pageable.unpaged())
            .getContent();
        StringBuilder sb = new StringBuilder();
        sb.append(CsvExport.line("id", "title", "room", "startTime", "endTime", "billableHours", "amount", "status", "paymentStatus", "approvedBy")).append('\n');
        for (Booking b : bookings) {
            long seconds = java.time.Duration.between(b.getStartTime(), b.getEndTime()).getSeconds();
            BigDecimal hours = BookingPricing.billableHours(seconds);
            String roomName = b.getRoom() != null ? b.getRoom().getName() : "";
            String approvedBy = b.getApprovedBy() != null
                ? (b.getApprovedBy().getFullName() != null ? b.getApprovedBy().getFullName() : b.getApprovedBy().getLogin())
                : "";
            sb
                .append(
                    CsvExport.line(
                        b.getId(),
                        b.getTitle(),
                        roomName,
                        b.getStartTime(),
                        b.getEndTime(),
                        hours,
                        b.getAmount(),
                        b.getStatus(),
                        b.getPaymentStatus(),
                        approvedBy
                    )
                )
                .append('\n');
        }
        return CsvExport.toUtf8BomBytes(sb.toString());
    }

    @Transactional(readOnly = true)
    public byte[] exportMonthlyRevenueCsv(String yearMonth) {
        RevenueReportDTO report = getMonthlyRevenue(yearMonth);
        StringBuilder sb = new StringBuilder();
        sb.append(CsvExport.line("metric", "value")).append('\n');
        sb.append(CsvExport.line("yearMonth", report.getYearMonth())).append('\n');
        sb.append(CsvExport.line("totalAmount", report.getTotalAmount())).append('\n');
        sb.append(CsvExport.line("equipmentCost", report.getEquipmentCost())).append('\n');
        sb.append(CsvExport.line("netAmount", report.getNetAmount())).append('\n');
        sb.append(CsvExport.line("totalBookings", report.getTotalBookings())).append('\n');
        sb.append(CsvExport.line("averageAmount", report.getAverageAmount())).append('\n');
        sb.append(CsvExport.line("cancelledCount", report.getCancelledCount())).append('\n');
        sb.append(CsvExport.line("cancellationRate", report.getCancellationRate())).append('\n');
        sb.append('\n');
        sb
            .append(CsvExport.line("roomId", "roomName", "bookingCount", "amount", "equipmentCost", "netAmount", "sharePercent"))
            .append('\n');
        for (RevenueByRoomDTO row : report.getByRoom()) {
            sb
                .append(
                    CsvExport.line(
                        row.getRoomId(),
                        row.getRoomName(),
                        row.getBookingCount(),
                        row.getAmount(),
                        row.getEquipmentCost(),
                        row.getNetAmount(),
                        row.getSharePercent()
                    )
                )
                .append('\n');
        }
        return CsvExport.toUtf8BomBytes(sb.toString());
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

        Map<Long, BigDecimal> equipmentByRoom = new HashMap<>();
        Map<Long, String> equipmentRoomNames = new HashMap<>();
        for (EquipmentPurchase purchase : equipmentPurchaseRepository.findFulfilledInRange(monthStart, monthEnd)) {
            if (purchase.getRoom() == null || purchase.getRoom().getId() == null) {
                continue;
            }
            Long roomId = purchase.getRoom().getId();
            BigDecimal line = (purchase.getUnitCost() != null ? purchase.getUnitCost() : BigDecimal.ZERO).multiply(
                BigDecimal.valueOf(purchase.getQuantity() != null ? purchase.getQuantity() : 0)
            );
            equipmentByRoom.merge(roomId, line, BigDecimal::add);
            if (purchase.getRoom().getName() != null) {
                equipmentRoomNames.putIfAbsent(roomId, purchase.getRoom().getName());
            }
        }
        Map<Long, BigDecimal> previousEquipmentByRoom = sumEquipmentCostByRoom(prevStart, prevEnd);
        BigDecimal equipmentCost = sumMap(equipmentByRoom);
        BigDecimal previousEquipmentCost = sumMap(previousEquipmentByRoom);

        RevenuePeriodDTO currentPeriod = buildPeriod(ym, bookings, cancelledCount, equipmentCost);
        RevenuePeriodDTO previousPeriod = buildPeriod(previousYm, previousBookings, previousCancelled, previousEquipmentCost);

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
            RevenueByRoomDTO roomRow = byRoom.computeIfAbsent(roomId, id -> newRoomRow(id, booking.getRoom().getName()));
            roomRow.setBookingCount(roomRow.getBookingCount() + 1);
            roomRow.setAmount(roomRow.getAmount().add(amount));

            LocalDate day = booking.getStartTime().atZone(APP_ZONE).toLocalDate();
            RevenueByDayDTO dayRow = byDay.get(day);
            if (dayRow != null) {
                dayRow.setBookingCount(dayRow.getBookingCount() + 1);
                dayRow.setAmount(dayRow.getAmount().add(amount));
            }
        }

        for (Map.Entry<Long, BigDecimal> entry : equipmentByRoom.entrySet()) {
            Long roomId = entry.getKey();
            RevenueByRoomDTO roomRow = byRoom.computeIfAbsent(
                roomId,
                id -> newRoomRow(id, equipmentRoomNames.getOrDefault(id, "Room #" + id))
            );
            roomRow.setEquipmentCost(entry.getValue());
        }

        List<RevenueByRoomDTO> roomList = new ArrayList<>(byRoom.values());
        for (RevenueByRoomDTO roomRow : roomList) {
            if (roomRow.getEquipmentCost() == null) {
                roomRow.setEquipmentCost(BigDecimal.ZERO);
            }
            roomRow.setNetAmount(roomRow.getAmount().subtract(roomRow.getEquipmentCost()));
            roomRow.setSharePercent(sharePercent(roomRow.getAmount(), total));
        }
        roomList.sort(Comparator.comparing(RevenueByRoomDTO::getAmount).reversed());

        RevenueReportDTO report = new RevenueReportDTO();
        report.setYearMonth(currentPeriod.getYearMonth());
        report.setTotalAmount(currentPeriod.getTotalAmount());
        report.setEquipmentCost(currentPeriod.getEquipmentCost());
        report.setNetAmount(currentPeriod.getNetAmount());
        report.setTotalBookings(currentPeriod.getTotalBookings());
        report.setAverageAmount(currentPeriod.getAverageAmount());
        report.setCancelledCount(currentPeriod.getCancelledCount());
        report.setCancellationRate(currentPeriod.getCancellationRate());
        report.setPrevious(previousPeriod);
        report.setByRoom(roomList);
        report.setByDay(new ArrayList<>(byDay.values()));
        return report;
    }

    /**
     * Paged/sorted slice of monthly by-room rows (in-memory over the month aggregate).
     * Optional {@code q} filters by room name.
     */
    @Transactional(readOnly = true)
    public Page<RevenueByRoomDTO> getMonthlyRevenueByRoom(String yearMonth, String q, Pageable pageable) {
        List<RevenueByRoomDTO> rooms = new ArrayList<>(getMonthlyRevenue(yearMonth).getByRoom());
        String needle = normalizeSearch(q);
        if (needle != null) {
            rooms.removeIf(row ->
                row.getRoomName() == null || !row.getRoomName().toLowerCase(Locale.ROOT).contains(needle)
            );
        }
        rooms.sort(byRoomComparator(pageable.getSort()));
        int start = (int) pageable.getOffset();
        if (start >= rooms.size()) {
            return new PageImpl<>(List.of(), pageable, rooms.size());
        }
        int end = Math.min(start + pageable.getPageSize(), rooms.size());
        return new PageImpl<>(rooms.subList(start, end), pageable, rooms.size());
    }

    private Map<Long, BigDecimal> sumEquipmentCostByRoom(Instant from, Instant to) {
        Map<Long, BigDecimal> byRoom = new HashMap<>();
        for (EquipmentPurchase purchase : equipmentPurchaseRepository.findFulfilledInRange(from, to)) {
            if (purchase.getRoom() == null || purchase.getRoom().getId() == null) {
                continue;
            }
            BigDecimal line = (purchase.getUnitCost() != null ? purchase.getUnitCost() : BigDecimal.ZERO).multiply(
                BigDecimal.valueOf(purchase.getQuantity() != null ? purchase.getQuantity() : 0)
            );
            byRoom.merge(purchase.getRoom().getId(), line, BigDecimal::add);
        }
        return byRoom;
    }

    private static BigDecimal sumMap(Map<Long, BigDecimal> map) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : map.values()) {
            total = total.add(value);
        }
        return total;
    }

    private static RevenueByRoomDTO newRoomRow(Long roomId, String roomName) {
        RevenueByRoomDTO dto = new RevenueByRoomDTO();
        dto.setRoomId(roomId);
        dto.setRoomName(roomName);
        dto.setBookingCount(0);
        dto.setAmount(BigDecimal.ZERO);
        dto.setEquipmentCost(BigDecimal.ZERO);
        dto.setNetAmount(BigDecimal.ZERO);
        dto.setSharePercent(BigDecimal.ZERO);
        return dto;
    }

    private static Comparator<RevenueByRoomDTO> byRoomComparator(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Comparator.comparing(RevenueByRoomDTO::getRoomId, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        Comparator<RevenueByRoomDTO> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<RevenueByRoomDTO> next = switch (order.getProperty()) {
                case "roomName" -> Comparator.comparing(
                    RevenueByRoomDTO::getRoomName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
                case "bookingCount" -> Comparator.comparingLong(RevenueByRoomDTO::getBookingCount);
                case "sharePercent" -> Comparator.comparing(
                    RevenueByRoomDTO::getSharePercent,
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
                case "equipmentCost" -> Comparator.comparing(
                    RevenueByRoomDTO::getEquipmentCost,
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
                case "netAmount" -> Comparator.comparing(
                    RevenueByRoomDTO::getNetAmount,
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
                case "amount" -> Comparator.comparing(
                    RevenueByRoomDTO::getAmount,
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
                default -> Comparator.comparing(
                    RevenueByRoomDTO::getRoomId,
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
            };
            if (order.isDescending()) {
                next = next.reversed();
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator != null
            ? comparator
            : Comparator.comparing(RevenueByRoomDTO::getRoomId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static String normalizeSearch(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim().toLowerCase(Locale.ROOT);
    }

    private static RevenuePeriodDTO buildPeriod(
        YearMonth ym,
        List<Booking> approved,
        long cancelledCount,
        BigDecimal equipmentCost
    ) {
        BigDecimal total = BigDecimal.ZERO;
        for (Booking booking : approved) {
            total = total.add(booking.getAmount() != null ? booking.getAmount() : BigDecimal.ZERO);
        }
        long bookings = approved.size();
        long attempts = bookings + cancelledCount;
        BigDecimal cost = equipmentCost != null ? equipmentCost : BigDecimal.ZERO;

        RevenuePeriodDTO period = new RevenuePeriodDTO();
        period.setYearMonth(ym.toString());
        period.setTotalAmount(total);
        period.setEquipmentCost(cost);
        period.setNetAmount(total.subtract(cost));
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
