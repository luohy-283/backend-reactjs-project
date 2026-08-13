package com.company.bookingroom.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class RevenueByRoomDTO implements Serializable {

    private Long roomId;
    private String roomName;
    private long bookingCount;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal equipmentCost = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
    /** Share of period total revenue (0–100). */
    private BigDecimal sharePercent = BigDecimal.ZERO;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public long getBookingCount() {
        return bookingCount;
    }

    public void setBookingCount(long bookingCount) {
        this.bookingCount = bookingCount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getEquipmentCost() {
        return equipmentCost;
    }

    public void setEquipmentCost(BigDecimal equipmentCost) {
        this.equipmentCost = equipmentCost;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getSharePercent() {
        return sharePercent;
    }

    public void setSharePercent(BigDecimal sharePercent) {
        this.sharePercent = sharePercent;
    }
}
