package com.company.bookingroom.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.company.bookingroom.IntegrationTest;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.UserService;
import com.company.bookingroom.service.dto.AdminUserDTO;
import com.company.bookingroom.service.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.function.Consumer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link UserResource} REST controller.
 */
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
@IntegrationTest
class UserResourceIT {

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String UPDATED_LOGIN = "jhipster";

    private static final Long DEFAULT_ID = 1L;

    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String UPDATED_EMAIL = "jhipster@localhost";

    private static final String DEFAULT_FULLNAME = "John Doe";
    private static final String UPDATED_FULLNAME = "JHipster User";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc restUserMockMvc;

    private User user;

    private Long numberOfUsers;

    @BeforeEach
    void countUsers() {
        numberOfUsers = userRepository.count();
    }

    /**
     * Create a User.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which has a required relationship to the User entity.
     */
    public static User createEntity() {
        User persistUser = new User();
        persistUser.setLogin(DEFAULT_LOGIN + RandomStringUtils.insecure().nextAlphabetic(5));
        persistUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        persistUser.setActivated(true);
        persistUser.setEmail(RandomStringUtils.insecure().nextAlphabetic(5) + DEFAULT_EMAIL);
        persistUser.setFullName(DEFAULT_FULLNAME);
        return persistUser;
    }

    /**
     * Setups the database with one user.
     */
    public static User initTestUser() {
        User persistUser = createEntity();
        persistUser.setLogin(DEFAULT_LOGIN);
        persistUser.setEmail(DEFAULT_EMAIL);
        return persistUser;
    }

    @BeforeEach
    void initTest() {
        user = initTestUser();
    }

    @AfterEach
    void cleanupAndCheck() {
        userService.deleteUser(DEFAULT_LOGIN);
        userService.deleteUser(UPDATED_LOGIN);
        userService.deleteUser(user.getLogin());
        userService.deleteUser("anotherlogin");
        assertThat(userRepository.count()).isEqualTo(numberOfUsers);
        numberOfUsers = null;
        cacheManager
            .getCacheNames()
            .stream()
            .map(cacheName -> this.cacheManager.getCache(cacheName))
            .filter(Objects::nonNull)
            .forEach(Cache::invalidate);
    }

    @Test
    @Transactional
    void createUser() throws Exception {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setFullName(DEFAULT_FULLNAME);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        var returnedUserDTO = om.readValue(
            restUserMockMvc
                .perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AdminUserDTO.class
        );

        User convertedUser = userMapper.userDTOToUser(returnedUserDTO);
        assertThat(convertedUser.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(convertedUser.getFullName()).isEqualTo(DEFAULT_FULLNAME);
        assertThat(convertedUser.getEmail()).isEqualTo(DEFAULT_EMAIL);
    }

    @Test
    @Transactional
    void createUserWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(DEFAULT_ID);
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setFullName(DEFAULT_FULLNAME);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        restUserMockMvc
            .perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isBadRequest());

        assertPersistedUsers(users -> assertThat(users).hasSize(databaseSizeBeforeCreate));
    }

    @Test
    @Transactional
    void createUserWithExistingLogin() throws Exception {
        userRepository.saveAndFlush(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setFullName(DEFAULT_FULLNAME);
        userDTO.setEmail("anothermail@localhost");
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        restUserMockMvc
            .perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isBadRequest());

        assertPersistedUsers(users -> assertThat(users).hasSize(databaseSizeBeforeCreate));
    }

    @Test
    @Transactional
    void createUserWithExistingEmail() throws Exception {
        userRepository.saveAndFlush(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("anotherlogin");
        userDTO.setFullName(DEFAULT_FULLNAME);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        restUserMockMvc
            .perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isBadRequest());

        assertPersistedUsers(users -> assertThat(users).hasSize(databaseSizeBeforeCreate));
    }

    @Test
    @Transactional
    void getAllUsers() throws Exception {
        userRepository.saveAndFlush(user);

        restUserMockMvc
            .perform(get("/api/admin/users?sort=id,desc").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
            .andExpect(jsonPath("$.[*].fullName").value(hasItem(DEFAULT_FULLNAME)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)));
    }

    @Test
    @Transactional
    void getUser() throws Exception {
        userRepository.saveAndFlush(user);

        assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin(), User.class)).isNull();

        restUserMockMvc
            .perform(get("/api/admin/users/{login}", user.getLogin()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.login").value(user.getLogin()))
            .andExpect(jsonPath("$.fullName").value(DEFAULT_FULLNAME))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL));

        assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin(), User.class)).isNotNull();
    }

    @Test
    @Transactional
    void getNonExistingUser() throws Exception {
        restUserMockMvc.perform(get("/api/admin/users/unknown")).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void updateUser() throws Exception {
        userRepository.saveAndFlush(user);
        int databaseSizeBeforeUpdate = userRepository.findAll().size();

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(updatedUser.getId());
        userDTO.setLogin(updatedUser.getLogin());
        userDTO.setFullName(UPDATED_FULLNAME);
        userDTO.setEmail(UPDATED_EMAIL);
        userDTO.setActivated(updatedUser.isActivated());
        userDTO.setCreatedBy(updatedUser.getCreatedBy());
        userDTO.setCreatedDate(updatedUser.getCreatedDate());
        userDTO.setLastModifiedBy(updatedUser.getLastModifiedBy());
        userDTO.setLastModifiedDate(updatedUser.getLastModifiedDate());
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        restUserMockMvc
            .perform(put("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isOk());

        assertPersistedUsers(users -> {
            assertThat(users).hasSize(databaseSizeBeforeUpdate);
            User testUser = users
                .stream()
                .filter(usr -> usr.getId().equals(updatedUser.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(testUser.getFullName()).isEqualTo(UPDATED_FULLNAME);
            assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
        });
    }

    @Test
    @Transactional
    void updateUserLogin() throws Exception {
        userRepository.saveAndFlush(user);
        int databaseSizeBeforeUpdate = userRepository.findAll().size();

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(updatedUser.getId());
        userDTO.setLogin(UPDATED_LOGIN);
        userDTO.setFullName(UPDATED_FULLNAME);
        userDTO.setEmail(UPDATED_EMAIL);
        userDTO.setActivated(updatedUser.isActivated());
        userDTO.setCreatedBy(updatedUser.getCreatedBy());
        userDTO.setCreatedDate(updatedUser.getCreatedDate());
        userDTO.setLastModifiedBy(updatedUser.getLastModifiedBy());
        userDTO.setLastModifiedDate(updatedUser.getLastModifiedDate());
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        restUserMockMvc
            .perform(put("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isOk());

        assertPersistedUsers(users -> {
            assertThat(users).hasSize(databaseSizeBeforeUpdate);
            User testUser = users
                .stream()
                .filter(usr -> usr.getId().equals(updatedUser.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(testUser.getLogin()).isEqualTo(UPDATED_LOGIN);
            assertThat(testUser.getFullName()).isEqualTo(UPDATED_FULLNAME);
            assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
        });
    }

    @Test
    @Transactional
    void updateUserExistingEmail() throws Exception {
        userRepository.saveAndFlush(user);

        User anotherUser = new User();
        anotherUser.setLogin("jhipster");
        anotherUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("jhipster@localhost");
        anotherUser.setFullName("Java Hipster");
        userRepository.saveAndFlush(anotherUser);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(updatedUser.getId());
        userDTO.setLogin(updatedUser.getLogin());
        userDTO.setFullName(updatedUser.getFullName());
        userDTO.setEmail("jhipster@localhost");
        userDTO.setActivated(updatedUser.isActivated());
        userDTO.setCreatedBy(updatedUser.getCreatedBy());
        userDTO.setCreatedDate(updatedUser.getCreatedDate());
        userDTO.setLastModifiedBy(updatedUser.getLastModifiedBy());
        userDTO.setLastModifiedDate(updatedUser.getLastModifiedDate());
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        restUserMockMvc
            .perform(put("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void updateUserExistingLogin() throws Exception {
        userRepository.saveAndFlush(user);

        User anotherUser = new User();
        anotherUser.setLogin("jhipster");
        anotherUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("jhipster@localhost");
        anotherUser.setFullName("Java Hipster");
        userRepository.saveAndFlush(anotherUser);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(updatedUser.getId());
        userDTO.setLogin("jhipster");
        userDTO.setFullName(updatedUser.getFullName());
        userDTO.setEmail(updatedUser.getEmail());
        userDTO.setActivated(updatedUser.isActivated());
        userDTO.setCreatedBy(updatedUser.getCreatedBy());
        userDTO.setCreatedDate(updatedUser.getCreatedDate());
        userDTO.setLastModifiedBy(updatedUser.getLastModifiedBy());
        userDTO.setLastModifiedDate(updatedUser.getLastModifiedDate());
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        restUserMockMvc
            .perform(put("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void deleteUser() throws Exception {
        userRepository.saveAndFlush(user);
        int databaseSizeBeforeDelete = userRepository.findAll().size();

        restUserMockMvc
            .perform(delete("/api/admin/users/{login}", user.getLogin()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin(), User.class)).isNull();

        assertPersistedUsers(users -> assertThat(users).hasSize(databaseSizeBeforeDelete - 1));
    }

    @Test
    void testUserEquals() throws Exception {
        TestUtil.equalsVerifier(User.class);
        User user1 = new User();
        user1.setId(DEFAULT_ID);
        User user2 = new User();
        user2.setId(user1.getId());
        assertThat(user1).isEqualTo(user2);
        user2.setId(2L);
        assertThat(user1).isNotEqualTo(user2);
        user1.setId(null);
        assertThat(user1).isNotEqualTo(user2);
    }

    private void assertPersistedUsers(Consumer<List<User>> userAssertion) {
        userAssertion.accept(userRepository.findAll());
    }
}
