package com.company.bookingroom.service.dto;

import com.company.bookingroom.domain.enumeration.EquipmentCategory;
import com.company.bookingroom.domain.enumeration.RoomLayoutType;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    private RoomLayoutType layoutType;

    @DecimalMin(value = "1.0")
    private BigDecimal floorWidthM;

    @DecimalMin(value = "1.0")
    private BigDecimal floorDepthM;

    /** Distinct OK equipment categories present in the room (list enrichment / filter). */
    private List<EquipmentCategory> equipmentCategories = new ArrayList<>();

    /** Distinct OK equipment catalog names (list enrichment / UI labels). */
    private List<String> equipmentNames = new ArrayList<>();

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

    public RoomLayoutType getLayoutType() {
        return layoutType;
    }

    public void setLayoutType(RoomLayoutType layoutType) {
        this.layoutType = layoutType;
    }

    public BigDecimal getFloorWidthM() {
        return floorWidthM;
    }

    public void setFloorWidthM(BigDecimal floorWidthM) {
        this.floorWidthM = floorWidthM;
    }

    public BigDecimal getFloorDepthM() {
        return floorDepthM;
    }

    public void setFloorDepthM(BigDecimal floorDepthM) {
        this.floorDepthM = floorDepthM;
    }

    public List<EquipmentCategory> getEquipmentCategories() {
        return equipmentCategories;
    }

    public void setEquipmentCategories(List<EquipmentCategory> equipmentCategories) {
        this.equipmentCategories = equipmentCategories != null ? equipmentCategories : new ArrayList<>();
    }

    public List<String> getEquipmentNames() {
        return equipmentNames;
    }

    public void setEquipmentNames(List<String> equipmentNames) {
        this.equipmentNames = equipmentNames != null ? equipmentNames : new ArrayList<>();
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
            ", layoutType='" + getLayoutType() + "'" +
            ", floorWidthM=" + getFloorWidthM() +
            ", floorDepthM=" + getFloorDepthM() +
            "}";
    }
}
