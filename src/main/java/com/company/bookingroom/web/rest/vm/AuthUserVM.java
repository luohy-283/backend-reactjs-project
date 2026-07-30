package com.company.bookingroom.web.rest.vm;

import com.company.bookingroom.service.dto.DepartmentDTO;

/**
 * Simplified user payload for the frontend auth contract.
 */
public class AuthUserVM {

    private Long id;
    private String email;
    private String fullName;
    private String role;
    private DepartmentDTO department;

    public AuthUserVM(Long id, String email, String fullName, String role, DepartmentDTO department) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public DepartmentDTO getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentDTO department) {
        this.department = department;
    }
}
