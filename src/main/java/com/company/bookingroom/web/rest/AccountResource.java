package com.company.bookingroom.web.rest;

import com.company.bookingroom.service.DepartmentChangeRequestService;
import com.company.bookingroom.service.InvoiceRevenueService;
import com.company.bookingroom.service.UserService;
import com.company.bookingroom.service.dto.AccountUpdateDTO;
import com.company.bookingroom.service.dto.AdminUserDTO;
import com.company.bookingroom.service.dto.BookingDTO;
import com.company.bookingroom.service.dto.DepartmentChangeRequestCreateDTO;
import com.company.bookingroom.service.dto.DepartmentChangeRequestDTO;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for the current user's account.
 */
@RestController
@RequestMapping("/api/account")
public class AccountResource {

    private final UserService userService;
    private final DepartmentChangeRequestService departmentChangeRequestService;
    private final InvoiceRevenueService invoiceRevenueService;

    public AccountResource(
        UserService userService,
        DepartmentChangeRequestService departmentChangeRequestService,
        InvoiceRevenueService invoiceRevenueService
    ) {
        this.userService = userService;
        this.departmentChangeRequestService = departmentChangeRequestService;
        this.invoiceRevenueService = invoiceRevenueService;
    }

    @GetMapping("")
    public ResponseEntity<AdminUserDTO> getAccount() {
        return ResponseUtil.wrapOrNotFound(userService.getAccount());
    }

    @PutMapping("")
    public ResponseEntity<AdminUserDTO> updateAccount(@Valid @RequestBody AccountUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateAccount(dto));
    }

    @PostMapping("/department-change-requests")
    public ResponseEntity<DepartmentChangeRequestDTO> requestDepartmentChange(
        @Valid @RequestBody DepartmentChangeRequestCreateDTO dto
    ) {
        return ResponseEntity.ok(departmentChangeRequestService.create(dto));
    }

    @GetMapping("/department-change-requests/pending")
    public ResponseEntity<DepartmentChangeRequestDTO> getMyPendingDepartmentChange() {
        Optional<DepartmentChangeRequestDTO> pending = departmentChangeRequestService.findMyPending();
        return pending.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/invoices")
    public Page<BookingDTO> getMyInvoices(
        @ParameterObject @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return invoiceRevenueService.findMyInvoices(pageable);
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<BookingDTO> getMyInvoice(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(invoiceRevenueService.findMyInvoice(id));
    }
}
