package com.company.bookingroom.service;

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
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final String ENTITY_NAME = "notification";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notifyUser(User recipient, NotificationType type, String title, String message, Long bookingId) {
        if (recipient == null) {
            return;
        }
        Notification n = new Notification();
        n.setUser(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setBookingId(bookingId);
        notificationRepository.save(n);
    }

    public void notifyAdmins(NotificationType type, String title, String message, Long bookingId) {
        List<User> admins = userRepository.findAllActivatedByAuthority(AuthoritiesConstants.ADMIN);
        for (User admin : admins) {
            notifyUser(admin, type, title, message, bookingId);
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
        notifyAdmins(
            NotificationType.DEPT_CHANGE_PENDING,
            "Yêu cầu đổi phòng ban",
            who + " yêu cầu chuyển sang " + dept + " (chờ duyệt).",
            requestId
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
