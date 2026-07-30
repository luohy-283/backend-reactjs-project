package com.company.bookingroom.web.rest;

import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.InvoiceRevenueService;
import com.company.bookingroom.service.dto.RevenueReportDTO;
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
}
