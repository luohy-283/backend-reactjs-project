package com.company.bookingroom.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically expires PENDING bookings whose start time has passed.
 */
@Component
public class BookingExpiryScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final BookingService bookingService;

    public BookingExpiryScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "300000")
    public void expirePastPendingBookings() {
        int count = bookingService.expirePastPending();
        if (count > 0) {
            LOG.debug("Scheduled expiry updated {} booking(s)", count);
        }
    }
}
