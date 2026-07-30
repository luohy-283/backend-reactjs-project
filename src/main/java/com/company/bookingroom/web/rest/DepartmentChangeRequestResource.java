package com.company.bookingroom.web.rest;

import com.company.bookingroom.domain.enumeration.DepartmentChangeRequestStatus;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.DepartmentChangeRequestService;
import com.company.bookingroom.service.dto.DepartmentChangeRequestDTO;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/department-change-requests")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class DepartmentChangeRequestResource {

    private final DepartmentChangeRequestService departmentChangeRequestService;

    public DepartmentChangeRequestResource(DepartmentChangeRequestService departmentChangeRequestService) {
        this.departmentChangeRequestService = departmentChangeRequestService;
    }

    @GetMapping("")
    public Page<DepartmentChangeRequestDTO> getAll(
        @RequestParam(required = false) DepartmentChangeRequestStatus status,
        @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return departmentChangeRequestService.findAll(status, pageable);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<DepartmentChangeRequestDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(departmentChangeRequestService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DepartmentChangeRequestDTO> reject(@PathVariable Long id) {
        return ResponseEntity.ok(departmentChangeRequestService.reject(id));
    }
}
