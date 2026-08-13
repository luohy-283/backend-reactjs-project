package com.company.bookingroom.service.dto;

import com.company.bookingroom.domain.enumeration.EquipmentCategory;
import com.company.bookingroom.domain.enumeration.RoomEquipmentStatus;
import jakarta.validation.constraints.Min;
import java.io.Serializable;
import java.math.BigDecimal;

public class RoomEquipmentDTO implements Serializable {

    private Long id;
    private Long roomId;
    private Long equipmentId;
    private String equipmentName;
    private EquipmentCategory category;
    private BigDecimal unitCost;

    @Min(0)
    private Integer quantity;

    private RoomEquipmentStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public void setCategory(EquipmentCategory category) {
        this.category = category;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public RoomEquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(RoomEquipmentStatus status) {
        this.status = status;
    }
}
