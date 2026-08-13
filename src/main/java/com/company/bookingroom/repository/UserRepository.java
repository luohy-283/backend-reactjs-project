package com.company.bookingroom.repository;

import com.company.bookingroom.domain.User;
import java.util.Collection;
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
     * Admin user list with optional text search, activated filter, and role filter.
     * {@code q} matches fullName, email, login, or department name (case-insensitive).
     * {@code roleAuthority} is {@code ROLE_*} when filtering ADMIN/MANAGER/STAFF;
     * {@code userOnly=true} means primary USER (has ROLE_USER and no higher role).
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
                or lower(u.fullName) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.email) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.login) like lower(concat('%', cast(:q as string), '%'))
                or (u.department is not null and lower(u.department.name) like lower(concat('%', cast(:q as string), '%')))
              )
              and (
                :roleAuthority is null
                or (
                  :userOnly = false
                  and exists (
                    select 1 from u.authorities a where a.name = :roleAuthority
                  )
                )
                or (
                  :userOnly = true
                  and exists (select 1 from u.authorities a where a.name = 'ROLE_USER')
                  and not exists (
                    select 1 from u.authorities a2
                    where a2.name in ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STAFF')
                  )
                )
              )
            """,
        countQuery = """
            select count(u) from User u
            left join u.department
            where (:activated is null or u.activated = :activated)
              and (
                :q is null or :q = ''
                or lower(u.fullName) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.email) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.login) like lower(concat('%', cast(:q as string), '%'))
                or (u.department is not null and lower(u.department.name) like lower(concat('%', cast(:q as string), '%')))
              )
              and (
                :roleAuthority is null
                or (
                  :userOnly = false
                  and exists (
                    select 1 from u.authorities a where a.name = :roleAuthority
                  )
                )
                or (
                  :userOnly = true
                  and exists (select 1 from u.authorities a where a.name = 'ROLE_USER')
                  and not exists (
                    select 1 from u.authorities a2
                    where a2.name in ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STAFF')
                  )
                )
              )
            """
    )
    Page<User> findAllManaged(
        @Param("q") String q,
        @Param("activated") Boolean activated,
        @Param("roleAuthority") String roleAuthority,
        @Param("userOnly") boolean userOnly,
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

    @Query(
        """
        select distinct u from User u
        join u.authorities a
        where a.name in :authorities
          and u.activated = true
        """
    )
    List<User> findAllActivatedByAnyAuthority(@Param("authorities") Collection<String> authorities);
}
