package com.company.bookingroom.security;

import com.company.bookingroom.domain.Authority;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;

/**
 * Maps between Spring {@code ROLE_*} authorities and frontend role strings (ADMIN|MANAGER|STAFF|USER).
 */
public final class RoleMapping {

    private RoleMapping() {}

    /**
     * Highest-priority role among authorities: ADMIN &gt; MANAGER &gt; STAFF &gt; USER.
     */
    public static String toFrontendRole(Collection<?> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return "USER";
        }
        boolean admin = false;
        boolean manager = false;
        boolean staff = false;
        for (Object raw : authorities) {
            String name = authorityName(raw);
            if (name == null) {
                continue;
            }
            if (AuthoritiesConstants.ADMIN.equals(name) || "ADMIN".equalsIgnoreCase(name)) {
                admin = true;
            } else if (AuthoritiesConstants.MANAGER.equals(name) || "MANAGER".equalsIgnoreCase(name)) {
                manager = true;
            } else if (AuthoritiesConstants.STAFF.equals(name) || "STAFF".equalsIgnoreCase(name)) {
                staff = true;
            }
        }
        if (admin) {
            return "ADMIN";
        }
        if (manager) {
            return "MANAGER";
        }
        if (staff) {
            return "STAFF";
        }
        return "USER";
    }

    /**
     * Primary {@code ROLE_*} plus {@code ROLE_USER} (always included for higher roles).
     */
    public static Set<String> toAuthorityNames(String role) {
        Set<String> names = new LinkedHashSet<>();
        names.add(AuthoritiesConstants.USER);
        if (role == null || role.isBlank()) {
            return names;
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        switch (normalized) {
            case "ADMIN" -> names.add(AuthoritiesConstants.ADMIN);
            case "MANAGER" -> names.add(AuthoritiesConstants.MANAGER);
            case "STAFF" -> names.add(AuthoritiesConstants.STAFF);
            default -> {
                // USER only
            }
        }
        return names;
    }

    /**
     * Normalize a list filter role to {@code ROLE_*} (ADMIN|MANAGER|STAFF|USER or ROLE_*).
     * Blank → null (no filter).
     */
    public static String toFilterAuthority(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        return switch (normalized) {
            case AuthoritiesConstants.ADMIN, AuthoritiesConstants.MANAGER, AuthoritiesConstants.STAFF, AuthoritiesConstants.USER ->
                normalized;
            default -> null;
        };
    }

    /** True when filtering for primary USER (excludes users who also have a higher role). */
    public static boolean isUserOnlyFilter(String roleAuthority) {
        return AuthoritiesConstants.USER.equals(roleAuthority);
    }

    private static String authorityName(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            return s;
        }
        if (raw instanceof Authority authority) {
            return authority.getName();
        }
        if (raw instanceof GrantedAuthority granted) {
            return granted.getAuthority();
        }
        return raw.toString();
    }
}
