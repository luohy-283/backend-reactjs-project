package com.company.bookingroom.service.dto;

import com.company.bookingroom.domain.enumeration.DepartmentChangeRequestStatus;
import java.io.Serializable;
import java.time.Instant;

public class DepartmentChangeRequestDTO implements Serializable {

    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private DepartmentDTO currentDepartment;
    private DepartmentDTO requestedDepartment;
    private DepartmentChangeRequestStatus status;
    private String reviewedByLogin;
    private Instant reviewedDate;
    private Instant createdDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public DepartmentDTO getCurrentDepartment() {
        return currentDepartment;
    }

    public void setCurrentDepartment(DepartmentDTO currentDepartment) {
        this.currentDepartment = currentDepartment;
    }

    public DepartmentDTO getRequestedDepartment() {
        return requestedDepartment;
    }

    public void setRequestedDepartment(DepartmentDTO requestedDepartment) {
        this.requestedDepartment = requestedDepartment;
    }

    public DepartmentChangeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(DepartmentChangeRequestStatus status) {
        this.status = status;
    }

    public String getReviewedByLogin() {
        return reviewedByLogin;
    }

    public void setReviewedByLogin(String reviewedByLogin) {
        this.reviewedByLogin = reviewedByLogin;
    }

    public Instant getReviewedDate() {
        return reviewedDate;
    }

    public void setReviewedDate(Instant reviewedDate) {
        this.reviewedDate = reviewedDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
}
