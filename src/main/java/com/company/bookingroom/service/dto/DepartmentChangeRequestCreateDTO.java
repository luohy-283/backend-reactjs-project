package com.company.bookingroom.service.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class DepartmentChangeRequestCreateDTO implements Serializable {

    @NotNull
    private Long requestedDepartmentId;

    public Long getRequestedDepartmentId() {
        return requestedDepartmentId;
    }

    public void setRequestedDepartmentId(Long requestedDepartmentId) {
        this.requestedDepartmentId = requestedDepartmentId;
    }
}
