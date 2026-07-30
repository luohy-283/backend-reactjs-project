package com.company.bookingroom.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RevenueReportDTO implements Serializable {

    private String yearMonth;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private long totalBookings;
    private BigDecimal averageAmount = BigDecimal.ZERO;
    private long cancelledCount;
    private BigDecimal cancellationRate = BigDecimal.ZERO;
    private RevenuePeriodDTO previous;
    private List<RevenueByRoomDTO> byRoom = new ArrayList<>();
    private List<RevenueByDayDTO> byDay = new ArrayList<>();

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }

    public void setAverageAmount(BigDecimal averageAmount) {
        this.averageAmount = averageAmount;
    }

    public long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public BigDecimal getCancellationRate() {
        return cancellationRate;
    }

    public void setCancellationRate(BigDecimal cancellationRate) {
        this.cancellationRate = cancellationRate;
    }

    public RevenuePeriodDTO getPrevious() {
        return previous;
    }

    public void setPrevious(RevenuePeriodDTO previous) {
        this.previous = previous;
    }

    public List<RevenueByRoomDTO> getByRoom() {
        return byRoom;
    }

    public void setByRoom(List<RevenueByRoomDTO> byRoom) {
        this.byRoom = byRoom;
    }

    public List<RevenueByDayDTO> getByDay() {
        return byDay;
    }

    public void setByDay(List<RevenueByDayDTO> byDay) {
        this.byDay = byDay;
    }
}
