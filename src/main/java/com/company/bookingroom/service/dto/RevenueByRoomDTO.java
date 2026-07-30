package com.company.bookingroom.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class RevenueByRoomDTO implements Serializable {

    private Long roomId;
    private String roomName;
    private long bookingCount;
    private BigDecimal amount = BigDecimal.ZERO;
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

    public BigDecimal getSharePercent() {
        return sharePercent;
    }

    public void setSharePercent(BigDecimal sharePercent) {
        this.sharePercent = sharePercent;
    }
}
