package com.company.bookingroom.service;

import com.company.bookingroom.domain.Room;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.security.SecurityUtils;

/**
 * Shared room visibility / booking-access rules.
 */
public final class RoomAccessRules {

    private RoomAccessRules() {}

    public static boolean isAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);
    }

    /**
     * Public room, or locked to the user's department. Admins can access all rooms.
     */
    public static boolean canAccess(Room room, User user) {
        if (room == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        if (room.getLockedDepartment() == null) {
            return true;
        }
        if (user == null || user.getDepartment() == null) {
            return false;
        }
        return room.getLockedDepartment().getId().equals(user.getDepartment().getId());
    }
}
