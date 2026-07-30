package com.company.bookingroom.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Room booking fee with 30-minute billing blocks (minimum 1 block).
 * <p>
 * billableHours = ceil(duration / 30min) × 0.5h
 * amount = pricePerHour × billableHours, rounded to whole VND.
 */
public final class BookingPricing {

    /** Billing quantum in seconds (30 minutes). */
    public static final long BLOCK_SECONDS = 30 * 60L;

    private BookingPricing() {}

    public static BigDecimal calculateAmount(BigDecimal pricePerHour, Instant start, Instant end) {
        if (pricePerHour == null || start == null || end == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long seconds = Duration.between(start, end).getSeconds();
        if (seconds <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal billableHours = billableHours(seconds);
        return pricePerHour.multiply(billableHours).setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Ceiling to 30-minute blocks; at least one block when duration &gt; 0.
     */
    public static BigDecimal billableHours(long durationSeconds) {
        if (durationSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        long blocks = (durationSeconds + BLOCK_SECONDS - 1) / BLOCK_SECONDS;
        return BigDecimal.valueOf(blocks).multiply(BigDecimal.valueOf(0.5));
    }
}
