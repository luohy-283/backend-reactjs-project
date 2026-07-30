package com.company.bookingroom.domain;

import static com.company.bookingroom.domain.BookingTestSamples.*;
import static com.company.bookingroom.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.company.bookingroom.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Booking.class);
        Booking booking1 = getBookingSample1();
        Booking booking2 = new Booking();
        assertThat(booking1).isNotEqualTo(booking2);

        booking2.setId(booking1.getId());
        assertThat(booking1).isEqualTo(booking2);

        booking2 = getBookingSample2();
        assertThat(booking1).isNotEqualTo(booking2);
    }

    @Test
    void roomTest() {
        Booking booking = getBookingRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        booking.setRoom(roomBack);
        assertThat(booking.getRoom()).isEqualTo(roomBack);

        booking.room(null);
        assertThat(booking.getRoom()).isNull();
    }
}
