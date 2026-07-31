package com.company.bookingroom.web.rest;

import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.InvoiceRevenueService;
import com.company.bookingroom.service.dto.RevenueByRoomDTO;
import com.company.bookingroom.service.dto.RevenueReportDTO;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/revenue")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class RevenueResource {

    private final InvoiceRevenueService invoiceRevenueService;

    public RevenueResource(InvoiceRevenueService invoiceRevenueService) {
        this.invoiceRevenueService = invoiceRevenueService;
    }

    @GetMapping("")
    public RevenueReportDTO getMonthlyRevenue(@RequestParam(required = false) String yearMonth) {
        return invoiceRevenueService.getMonthlyRevenue(yearMonth);
    }

    @GetMapping("/by-room")
    public Page<RevenueByRoomDTO> getMonthlyRevenueByRoom(
        @RequestParam(required = false) String yearMonth,
        @RequestParam(required = false) String q,
        @ParameterObject @PageableDefault(size = 20, sort = "amount", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return invoiceRevenueService.getMonthlyRevenueByRoom(yearMonth, q, pageable);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMonthlyRevenue(@RequestParam(required = false) String yearMonth) {
        byte[] csv = invoiceRevenueService.exportMonthlyRevenueCsv(yearMonth);
        String ym = yearMonth == null || yearMonth.isBlank() ? "current" : yearMonth;
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"revenue-" + ym + ".csv\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv);
    }
}
