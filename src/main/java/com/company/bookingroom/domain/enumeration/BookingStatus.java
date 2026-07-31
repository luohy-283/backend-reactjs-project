package com.company.bookingroom.domain.enumeration;

/**
 * The BookingStatus enumeration.
 */
public enum BookingStatus {
    PENDING,
    APPROVED,
    CANCELLED,
    /** PENDING that was not approved before start time — moved to history. */
    EXPIRED,
}
