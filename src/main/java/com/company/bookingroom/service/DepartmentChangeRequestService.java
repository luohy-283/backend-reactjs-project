package com.company.bookingroom.service;

import com.company.bookingroom.domain.Department;
import com.company.bookingroom.domain.DepartmentChangeRequest;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.domain.enumeration.DepartmentChangeRequestStatus;
import com.company.bookingroom.repository.DepartmentChangeRequestRepository;
import com.company.bookingroom.repository.DepartmentRepository;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.SecurityUtils;
import com.company.bookingroom.service.dto.DepartmentChangeRequestCreateDTO;
import com.company.bookingroom.service.dto.DepartmentChangeRequestDTO;
import com.company.bookingroom.service.dto.DepartmentDTO;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepartmentChangeRequestService {

    private static final String ENTITY_NAME = "departmentChangeRequest";

    private final DepartmentChangeRequestRepository requestRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public DepartmentChangeRequestService(
        DepartmentChangeRequestRepository requestRepository,
        DepartmentRepository departmentRepository,
        UserRepository userRepository,
        CacheManager cacheManager
    ) {
        this.requestRepository = requestRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    public DepartmentChangeRequestDTO create(DepartmentChangeRequestCreateDTO dto) {
        User current = requireCurrentUser();
        if (requestRepository.existsByUserIdAndStatus(current.getId(), DepartmentChangeRequestStatus.PENDING)) {
            throw new BadRequestAlertException(
                "Bạn đã có yêu cầu đổi phòng ban đang chờ duyệt",
                ENTITY_NAME,
                "pendingexists"
            );
        }
        Department target = departmentRepository
            .findById(dto.getRequestedDepartmentId())
            .orElseThrow(() -> new BadRequestAlertException("Department not found", ENTITY_NAME, "departmentnotfound"));

        if (current.getDepartment() != null && current.getDepartment().getId().equals(target.getId())) {
            throw new BadRequestAlertException(
                "Phòng ban yêu cầu trùng với phòng ban hiện tại",
                ENTITY_NAME,
                "sameDepartment"
            );
        }

        DepartmentChangeRequest request = new DepartmentChangeRequest();
        request.setUser(current);
        request.setRequestedDepartment(target);
        request.setStatus(DepartmentChangeRequestStatus.PENDING);
        request = requestRepository.save(request);
        return toDto(requestRepository.findOneWithDetails(request.getId()).orElse(request));
    }

    @Transactional(readOnly = true)
    public Optional<DepartmentChangeRequestDTO> findMyPending() {
        User current = requireCurrentUser();
        return requestRepository
            .findFirstByUserIdAndStatusOrderByCreatedDateDesc(current.getId(), DepartmentChangeRequestStatus.PENDING)
            .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentChangeRequestDTO> findAll(DepartmentChangeRequestStatus status, Pageable pageable) {
        return requestRepository.findAllByStatusOptional(status, pageable).map(this::toDto);
    }

    public DepartmentChangeRequestDTO approve(Long id) {
        DepartmentChangeRequest request = requestRepository
            .findOneWithDetails(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
        if (request.getStatus() != DepartmentChangeRequestStatus.PENDING) {
            throw new BadRequestAlertException("Only PENDING requests can be approved", ENTITY_NAME, "invalidstatus");
        }
        User reviewer = requireCurrentUser();
        User targetUser = request.getUser();
        clearUserCaches(targetUser);
        targetUser.setDepartment(request.getRequestedDepartment());
        userRepository.save(targetUser);
        clearUserCaches(targetUser);

        request.setStatus(DepartmentChangeRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedDate(Instant.now());
        return toDto(requestRepository.save(request));
    }

    public DepartmentChangeRequestDTO reject(Long id) {
        DepartmentChangeRequest request = requestRepository
            .findOneWithDetails(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
        if (request.getStatus() != DepartmentChangeRequestStatus.PENDING) {
            throw new BadRequestAlertException("Only PENDING requests can be rejected", ENTITY_NAME, "invalidstatus");
        }
        User reviewer = requireCurrentUser();
        request.setStatus(DepartmentChangeRequestStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedDate(Instant.now());
        return toDto(requestRepository.save(request));
    }

    private DepartmentChangeRequestDTO toDto(DepartmentChangeRequest request) {
        DepartmentChangeRequestDTO dto = new DepartmentChangeRequestDTO();
        dto.setId(request.getId());
        dto.setStatus(request.getStatus());
        dto.setReviewedDate(request.getReviewedDate());
        dto.setCreatedDate(request.getCreatedDate());
        if (request.getUser() != null) {
            dto.setUserId(request.getUser().getId());
            dto.setUserEmail(request.getUser().getEmail());
            dto.setUserFullName(request.getUser().getFullName());
            dto.setCurrentDepartment(toDepartmentDto(request.getUser().getDepartment()));
        }
        dto.setRequestedDepartment(toDepartmentDto(request.getRequestedDepartment()));
        if (request.getReviewedBy() != null) {
            dto.setReviewedByLogin(request.getReviewedBy().getLogin());
        }
        return dto;
    }

    private static DepartmentDTO toDepartmentDto(Department department) {
        if (department == null) {
            return null;
        }
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setCode(department.getCode());
        dto.setName(department.getName());
        return dto;
    }

    private User requireCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user not found in token", ENTITY_NAME, "usernotfound")
        );
        return userRepository
            .findOneWithAuthoritiesByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evictIfPresent(user.getLogin());
        if (user.getEmail() != null) {
            Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evictIfPresent(user.getEmail());
        }
    }
}
