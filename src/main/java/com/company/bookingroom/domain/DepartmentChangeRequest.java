package com.company.bookingroom.domain;

import com.company.bookingroom.domain.enumeration.DepartmentChangeRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * User request to change department — requires admin approval.
 */
@Entity
@Table(name = "department_change_request")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DepartmentChangeRequest extends AbstractAuditingEntity<Long> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_department_id", nullable = false)
    private Department requestedDepartment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DepartmentChangeRequestStatus status = DepartmentChangeRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_date")
    private Instant reviewedDate;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Department getRequestedDepartment() {
        return requestedDepartment;
    }

    public void setRequestedDepartment(Department requestedDepartment) {
        this.requestedDepartment = requestedDepartment;
    }

    public DepartmentChangeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(DepartmentChangeRequestStatus status) {
        this.status = status;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedDate() {
        return reviewedDate;
    }

    public void setReviewedDate(Instant reviewedDate) {
        this.reviewedDate = reviewedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DepartmentChangeRequest)) {
            return false;
        }
        return id != null && id.equals(((DepartmentChangeRequest) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
