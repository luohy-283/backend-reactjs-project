package com.company.bookingroom.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.bookingroom.IntegrationTest;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.dto.AdminUserDTO;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link UserService}.
 */
@IntegrationTest
@Transactional
class UserServiceIT {

    private static final String DEFAULT_LOGIN = "johndoe_service";

    private static final String DEFAULT_EMAIL = "johndoe_service@localhost";

    private static final String DEFAULT_FULLNAME = "John Doe";

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private Long numberOfUsers;

    @BeforeEach
    void countUsers() {
        numberOfUsers = userRepository.count();
    }

    @AfterEach
    void cleanupAndCheck() {
        cacheManager
            .getCacheNames()
            .stream()
            .map(cacheName -> this.cacheManager.getCache(cacheName))
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
        userService.deleteUser(DEFAULT_LOGIN);
        assertThat(userRepository.count()).isEqualTo(numberOfUsers);
        numberOfUsers = null;
    }

    @Test
    @Transactional
    void assertThatUserCanBeCreated() {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setFullName(DEFAULT_FULLNAME);
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        User created = userService.createUser(userDTO);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(created.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(created.getFullName()).isEqualTo(DEFAULT_FULLNAME);
        assertThat(created.isActivated()).isTrue();
        assertThat(created.getPassword()).isNotBlank();
    }

    @Test
    @Transactional
    void assertThatUserCanBeFoundByLogin() {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setFullName(DEFAULT_FULLNAME);
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));
        userService.createUser(userDTO);

        assertThat(userService.getUserWithAuthoritiesByLogin(DEFAULT_LOGIN)).isPresent();
    }
}
