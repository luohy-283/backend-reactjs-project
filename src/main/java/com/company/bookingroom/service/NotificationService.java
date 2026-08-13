package com.company.bookingroom.service;

import com.company.bookingroom.domain.Authority;
import com.company.bookingroom.domain.Booking;
import com.company.bookingroom.domain.Notification;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.domain.enumeration.NotificationType;
import com.company.bookingroom.repository.NotificationRepository;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.security.SecurityUtils;
import com.company.bookingroom.service.dto.NotificationDTO;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final String ENTITY_NAME = "notification";

    /** Privileged inbox is actionable-only; outcome types go to non-privileged bookers/requesters. */
    private static final Set<NotificationType> PRIVILEGED_ACTIONABLE_TYPES = EnumSet.of(
        NotificationType.BOOKING_PENDING,
        NotificationType.DEPT_CHANGE_PENDING
    );

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * @param refId booking id for BOOKING_* types, department-change-request id for DEPT_CHANGE_* (nullable for outcomes)
     */
    public void notifyUser(User recipient, NotificationType type, String title, String message, Long refId) {
        if (recipient == null) {
            return;
        }
        User resolved = resolveWithAuthorities(recipient);
        // ADMIN/MANAGER only receive PENDING (actionable) notices — never user-outcome types.
        if (isPrivilegedInbox(resolved) && !PRIVILEGED_ACTIONABLE_TYPES.contains(type)) {
            return;
        }
        Notification n = new Notification();
        n.setUser(resolved);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setBookingId(refId);
        Notification saved = notificationRepository.save(n);
        pushRealtime(resolved.getLogin(), toDto(saved));
    }

    private void pushRealtime(String login, NotificationDTO dto) {
        if (login == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(login, "/queue/notifications", dto);
    }

    public void notifyAdmins(NotificationType type, String title, String message, Long refId) {
        notifyAdmins(type, title, message, refId, null);
    }

    public void notifyAdmins(NotificationType type, String title, String message, Long refId, User exclude) {
        List<User> recipients = userRepository.findAllActivatedByAnyAuthority(
            List.of(AuthoritiesConstants.ADMIN, AuthoritiesConstants.MANAGER)
        );
        for (User recipient : recipients) {
            if (exclude != null && Objects.equals(exclude.getId(), recipient.getId())) {
                continue;
            }
            notifyUser(recipient, type, title, message, refId);
        }
    }

    public void notifyBookingPending(Booking booking) {
        String roomName = booking.getRoom() != null ? booking.getRoom().getName() : "phòng";
        String requester = booking.getUser() != null ? booking.getUser().getLogin() : "user";
        notifyAdmins(
            NotificationType.BOOKING_PENDING,
            "Yêu cầu đặt phòng mới",
            requester + " vừa đặt \"" + booking.getTitle() + "\" tại " + roomName + " (chờ duyệt).",
            booking.getId()
        );
    }

    public void notifyBookingApproved(Booking booking) {
        notifyUser(
            booking.getUser(),
            NotificationType.BOOKING_APPROVED,
            "Lịch đặt đã được duyệt",
            "\"" + booking.getTitle() + "\" đã được duyệt.",
            booking.getId()
        );
    }

    public void notifyBookingRejected(Booking booking) {
        notifyUser(
            booking.getUser(),
            NotificationType.BOOKING_REJECTED,
            "Lịch đặt bị từ chối",
            "\"" + booking.getTitle() + "\" đã bị từ chối.",
            booking.getId()
        );
    }

    public void notifyBookingCancelled(Booking booking) {
        notifyUser(
            booking.getUser(),
            NotificationType.BOOKING_CANCELLED,
            "Lịch đặt đã bị hủy",
            "\"" + booking.getTitle() + "\" đã bị hủy bởi admin.",
            booking.getId()
        );
    }

    public void notifyBookingExpired(Booking booking) {
        notifyUser(
            booking.getUser(),
            NotificationType.BOOKING_EXPIRED,
            "Yêu cầu đặt phòng đã hết hạn",
            "\"" + booking.getTitle() + "\" không được duyệt trước giờ bắt đầu và đã hết hạn.",
            booking.getId()
        );
    }

    public void notifyDeptChangePending(User requester, String targetDepartmentName, Long requestId) {
        String who = requester != null ? requester.getLogin() : "user";
        String dept = targetDepartmentName != null ? targetDepartmentName : "phòng ban mới";
        // Skip self when an admin requests a dept change (avoid self-inbox noise).
        notifyAdmins(
            NotificationType.DEPT_CHANGE_PENDING,
            "Yêu cầu đổi phòng ban",
            who + " yêu cầu chuyển sang " + dept + " (chờ duyệt).",
            requestId,
            requester
        );
    }

    public void notifyDeptChangeApproved(User requester, String targetDepartmentName) {
        String dept = targetDepartmentName != null ? targetDepartmentName : "phòng ban mới";
        notifyUser(
            requester,
            NotificationType.DEPT_CHANGE_APPROVED,
            "Đổi phòng ban đã được duyệt",
            "Yêu cầu chuyển sang " + dept + " đã được duyệt.",
            null
        );
    }

    public void notifyDeptChangeRejected(User requester, String targetDepartmentName) {
        String dept = targetDepartmentName != null ? targetDepartmentName : "phòng ban mới";
        notifyUser(
            requester,
            NotificationType.DEPT_CHANGE_REJECTED,
            "Đổi phòng ban bị từ chối",
            "Yêu cầu chuyển sang " + dept + " đã bị từ chối.",
            null
        );
    }

    /** Remove admin "chờ duyệt" alerts once the booking is no longer PENDING. */
    public void clearPendingBookingNotifications(Long bookingId) {
        if (bookingId == null) {
            return;
        }
        notificationRepository.deleteByTypeAndRefId(NotificationType.BOOKING_PENDING, bookingId);
    }

    /** Remove admin dept-change alerts once the request is approved/rejected. */
    public void clearPendingDeptChangeNotifications(Long requestId) {
        if (requestId == null) {
            return;
        }
        notificationRepository.deleteByTypeAndRefId(NotificationType.DEPT_CHANGE_PENDING, requestId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> findMine(Pageable pageable) {
        String login = requireLogin();
        return notificationRepository.findByUserLoginOrderByCreatedDateDesc(login, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserLoginAndReadDateIsNull(requireLogin());
    }

    public NotificationDTO markRead(Long id) {
        String login = requireLogin();
        Notification n = notificationRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Notification not found", ENTITY_NAME, "idnotfound"));
        if (!login.equals(n.getUser().getLogin())) {
            throw new BadRequestAlertException("Forbidden", ENTITY_NAME, "forbidden");
        }
        if (n.getReadDate() == null) {
            n.setReadDate(Instant.now());
            n = notificationRepository.save(n);
        }
        return toDto(n);
    }

    public int markAllRead() {
        return notificationRepository.markAllReadForLogin(requireLogin());
    }

    private User resolveWithAuthorities(User user) {
        if (user.getLogin() == null) {
            return user;
        }
        return userRepository.findOneWithAuthoritiesByLogin(user.getLogin()).orElse(user);
    }

    private boolean isPrivilegedInbox(User user) {
        if (user.getAuthorities() == null) {
            return false;
        }
        for (Authority authority : user.getAuthorities()) {
            String name = authority.getName();
            if (AuthoritiesConstants.ADMIN.equals(name) || AuthoritiesConstants.MANAGER.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private NotificationDTO toDto(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setType(n.getType());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setBookingId(n.getBookingId());
        dto.setReadDate(n.getReadDate());
        dto.setCreatedDate(n.getCreatedDate());
        dto.setRead(n.isRead());
        return dto;
    }

    private String requireLogin() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user not found in token", ENTITY_NAME, "usernotfound")
        );
    }
}
