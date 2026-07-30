package com.company.bookingroom.service;

import com.company.bookingroom.domain.Department;
import com.company.bookingroom.repository.DepartmentRepository;
import com.company.bookingroom.service.dto.DepartmentDTO;
import com.company.bookingroom.service.mapper.DepartmentMapper;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepartmentService {

    private static final String ENTITY_NAME = "department";

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO> findAll() {
        return departmentRepository.findAll().stream().map(departmentMapper::toDto).toList();
    }

    public DepartmentDTO save(DepartmentDTO dto) {
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new department cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Department department = departmentMapper.toEntity(dto);
        department = departmentRepository.save(department);
        return departmentMapper.toDto(department);
    }

    public DepartmentDTO update(DepartmentDTO dto) {
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!departmentRepository.existsById(dto.getId())) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Department department = departmentMapper.toEntity(dto);
        department = departmentRepository.save(department);
        return departmentMapper.toDto(department);
    }
}
