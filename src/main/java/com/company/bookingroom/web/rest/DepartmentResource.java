package com.company.bookingroom.web.rest;

import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.DepartmentService;
import com.company.bookingroom.service.dto.DepartmentDTO;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentResource {

    private final DepartmentService departmentService;

    public DepartmentResource(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("")
    public List<DepartmentDTO> getAll() {
        return departmentService.findAll();
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<DepartmentDTO> create(@Valid @RequestBody DepartmentDTO dto) throws URISyntaxException {
        DepartmentDTO result = departmentService.save(dto);
        return ResponseEntity.created(new URI("/api/departments/" + result.getId())).body(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<DepartmentDTO> update(@PathVariable Long id, @Valid @RequestBody DepartmentDTO dto) {
        dto.setId(id);
        return ResponseEntity.ok(departmentService.update(dto));
    }
}
