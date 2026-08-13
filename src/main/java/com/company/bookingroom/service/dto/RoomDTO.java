package com.company.bookingroom.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A DTO for the {@link com.company.bookingroom.domain.Room} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String name;

    @NotNull
    @Min(value = 1)
    private Integer capacity;

    private Boolean isActive;

    private DepartmentDTO lockedDepartment;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal pricePerHour;

    private Boolean isVip;

    @Size(max = 500)
    private String vipAmenities;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public DepartmentDTO getLockedDepartment() {
        return lockedDepartment;
    }

    public void setLockedDepartment(DepartmentDTO lockedDepartment) {
        this.lockedDepartment = lockedDepartment;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public Boolean getIsVip() {
        return isVip;
    }

    public void setIsVip(Boolean isVip) {
        this.isVip = isVip;
    }

    public String getVipAmenities() {
        return vipAmenities;
    }

    public void setVipAmenities(String vipAmenities) {
        this.vipAmenities = vipAmenities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomDTO)) {
            return false;
        }

        RoomDTO roomDTO = (RoomDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roomDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", capacity=" + getCapacity() +
            ", isActive='" + getIsActive() + "'" +
            ", lockedDepartment=" + getLockedDepartment() +
            ", pricePerHour=" + getPricePerHour() +
            ", isVip='" + getIsVip() + "'" +
            ", vipAmenities='" + getVipAmenities() + "'" +
            "}";
    }
}
