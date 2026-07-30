package com.company.bookingroom.service;

import com.company.bookingroom.domain.Authority;
import com.company.bookingroom.domain.Department;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.repository.AuthorityRepository;
import com.company.bookingroom.repository.DepartmentRepository;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.SecurityUtils;
import com.company.bookingroom.service.dto.AccountUpdateDTO;
import com.company.bookingroom.service.dto.AdminUserDTO;
import com.company.bookingroom.service.dto.UserDTO;
import com.company.bookingroom.web.rest.errors.BadRequestAlertException;
import com.company.bookingroom.web.rest.errors.EmailAlreadyUsedException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.security.RandomUtil;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    private static final String ENTITY_NAME = "userManagement";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    private final DepartmentRepository departmentRepository;
    private final CacheManager cacheManager;

    public UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthorityRepository authorityRepository,
        DepartmentRepository departmentRepository,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.departmentRepository = departmentRepository;
        this.cacheManager = cacheManager;
    }

    public User createUser(AdminUserDTO userDTO) {
        User user = new User();
        String login = userDTO.getLogin() != null ? userDTO.getLogin().toLowerCase() : userDTO.getEmail().toLowerCase();
        user.setLogin(login);
        user.setFullName(userDTO.getFullName());
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail().toLowerCase());
        }
        String rawPassword = userDTO.getPassword() != null && !userDTO.getPassword().isBlank()
            ? userDTO.getPassword()
            : RandomUtil.generatePassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActivated(true);
        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO
                .getAuthorities()
                .stream()
                .map(authorityRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        user.setDepartment(resolveDepartment(userDTO));
        userRepository.save(user);
        this.clearUserCaches(user);
        LOG.debug("Created Information for User: {}", user);
        return user;
    }

    public Optional<AdminUserDTO> updateUser(AdminUserDTO userDTO) {
        return Optional.of(userRepository.findById(userDTO.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(user -> {
                this.clearUserCaches(user);
                user.setLogin(userDTO.getLogin().toLowerCase());
                user.setFullName(userDTO.getFullName());
                if (userDTO.getEmail() != null) {
                    user.setEmail(userDTO.getEmail().toLowerCase());
                }
                user.setActivated(userDTO.isActivated());
                if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
                    user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                }
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                if (userDTO.getAuthorities() != null) {
                    userDTO
                        .getAuthorities()
                        .stream()
                        .map(authorityRepository::findById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .forEach(managedAuthorities::add);
                }
                user.setDepartment(resolveDepartment(userDTO));
                userRepository.save(user);
                this.clearUserCaches(user);
                LOG.debug("Changed Information for User: {}", user);
                return user;
            })
            .map(AdminUserDTO::new);
    }

    /**
     * Soft-delete: deactivate the user.
     */
    public void deleteUser(String login) {
        userRepository.findOneByLogin(login).ifPresent(user -> {
            this.clearUserCaches(user);
            user.setActivated(false);
            userRepository.save(user);
            this.clearUserCaches(user);
            LOG.debug("Deactivated User: {}", user);
        });
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllPublicUsers(Pageable pageable) {
        return userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable).map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
    }

    @Transactional(readOnly = true)
    public Optional<AdminUserDTO> getAccount() {
        return getUserWithAuthorities().map(AdminUserDTO::new);
    }

    public AdminUserDTO updateAccount(AccountUpdateDTO dto) {
        User user = getUserWithAuthorities().orElseThrow(() ->
            new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound")
        );
        this.clearUserCaches(user);
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null) {
            String email = dto.getEmail().toLowerCase();
            Optional<User> existing = userRepository.findOneByEmailIgnoreCase(email);
            if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
                throw new EmailAlreadyUsedException();
            }
            user.setEmail(email);
            user.setLogin(email);
        }
        userRepository.save(user);
        this.clearUserCaches(user);
        return new AdminUserDTO(user);
    }

    @Transactional(readOnly = true)
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).toList();
    }

    private Department resolveDepartment(AdminUserDTO userDTO) {
        if (userDTO.getDepartment() == null || userDTO.getDepartment().getId() == null) {
            return null;
        }
        return departmentRepository
            .findById(userDTO.getDepartment().getId())
            .orElseThrow(() -> new BadRequestAlertException("Department not found", ENTITY_NAME, "departmentnotfound"));
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evictIfPresent(user.getLogin());
        if (user.getEmail() != null) {
            Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evictIfPresent(user.getEmail());
        }
    }
}
