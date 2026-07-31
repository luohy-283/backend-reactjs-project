package com.company.bookingroom.repository;

import com.company.bookingroom.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";

    Optional<User> findOneByEmailIgnoreCase(String email);

    Optional<User> findOneByLogin(String login);

    @EntityGraph(attributePaths = { "authorities", "department" })
    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE, unless = "#result == null")
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = { "authorities", "department" })
    @Cacheable(cacheNames = USERS_BY_EMAIL_CACHE, unless = "#result == null")
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    /**
     * Admin user list with optional text search and activated filter.
     * {@code q} matches fullName, email, login, or department name (case-insensitive).
     * Do not EntityGraph-fetch {@code authorities} here — collection fetch + pagination
     * fails with hibernate.query.fail_on_pagination_over_collection_fetch.
     * Authorities load via {@code @BatchSize} when mapping to AdminUserDTO.
     */
    @Query(
        value = """
            select u from User u
            left join u.department
            where (:activated is null or u.activated = :activated)
              and (
                :q is null or :q = ''
                or lower(u.fullName) like lower(concat('%', :q, '%'))
                or lower(u.email) like lower(concat('%', :q, '%'))
                or lower(u.login) like lower(concat('%', :q, '%'))
                or (u.department is not null and lower(u.department.name) like lower(concat('%', :q, '%')))
              )
            """,
        countQuery = """
            select count(u) from User u
            left join u.department
            where (:activated is null or u.activated = :activated)
              and (
                :q is null or :q = ''
                or lower(u.fullName) like lower(concat('%', :q, '%'))
                or lower(u.email) like lower(concat('%', :q, '%'))
                or lower(u.login) like lower(concat('%', :q, '%'))
                or (u.department is not null and lower(u.department.name) like lower(concat('%', :q, '%')))
              )
            """
    )
    Page<User> findAllManaged(
        @Param("q") String q,
        @Param("activated") Boolean activated,
        Pageable pageable
    );

    @Query(
        """
        select distinct u from User u
        join u.authorities a
        where a.name = :authority
          and u.activated = true
        """
    )
    List<User> findAllActivatedByAuthority(@Param("authority") String authority);
}
