package com.company.bookingroom.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class RevenueByDayDTO implements Serializable {

    private String date;
    private long bookingCount;
    private BigDecimal amount = BigDecimal.ZERO;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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
}
