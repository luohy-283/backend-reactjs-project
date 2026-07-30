package com.company.bookingroom.service.mapper;

import com.company.bookingroom.domain.Department;
import com.company.bookingroom.service.dto.DepartmentDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepartmentMapper extends EntityMapper<DepartmentDTO, Department> {}
