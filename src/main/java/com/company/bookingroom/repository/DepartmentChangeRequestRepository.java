package com.company.bookingroom.repository;

import com.company.bookingroom.domain.DepartmentChangeRequest;
import com.company.bookingroom.domain.enumeration.DepartmentChangeRequestStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentChangeRequestRepository extends JpaRepository<DepartmentChangeRequest, Long> {
    boolean existsByUserIdAndStatus(Long userId, DepartmentChangeRequestStatus status);

    Optional<DepartmentChangeRequest> findFirstByUserIdAndStatusOrderByCreatedDateDesc(
        Long userId,
        DepartmentChangeRequestStatus status
    );

    @Query(
        value = """
            select r from DepartmentChangeRequest r
            left join fetch r.user u
            left join fetch u.department
            left join fetch r.requestedDepartment
            left join fetch r.reviewedBy
            where (:status is null or r.status = :status)
            """,
        countQuery = """
            select count(r) from DepartmentChangeRequest r
            where (:status is null or r.status = :status)
            """
    )
    Page<DepartmentChangeRequest> findAllByStatusOptional(
        @Param("status") DepartmentChangeRequestStatus status,
        Pageable pageable
    );

    @Query(
        """
        select r from DepartmentChangeRequest r
        left join fetch r.user u
        left join fetch u.department
        left join fetch r.requestedDepartment
        left join fetch r.reviewedBy
        where r.id = :id
        """
    )
    Optional<DepartmentChangeRequest> findOneWithDetails(@Param("id") Long id);
}
