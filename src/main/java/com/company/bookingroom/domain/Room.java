package com.company.bookingroom.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Room.
 */
@Entity
@Table(name = "room")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Room implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @NotNull
    @Min(value = 1)
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * When null, room is public (visible to all departments).
     * When set, only that department (and admins) can see/book it.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "locked_department_id")
    private Department lockedDepartment;

    /**
     * Unit price in VND per hour.
     */
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "price_per_hour", precision = 19, scale = 2, nullable = false)
    private BigDecimal pricePerHour;

    @NotNull
    @Column(name = "is_vip", nullable = false)
    private Boolean isVip = false;

    /**
     * CSV of VIP amenity codes, e.g. VIDEO_4K,SOUNDPROOF,CATERING,DEDICATED_SUPPORT,PRIVACY_GLASS.
     */
    @Size(max = 500)
    @Column(name = "vip_amenities", length = 500)
    private String vipAmenities;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Room id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Room name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return this.capacity;
    }

    public Room capacity(Integer capacity) {
        this.setCapacity(capacity);
        return this;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Boolean getIsActive() {
        return this.isActive;
    }

    public Room isActive(Boolean isActive) {
        this.setIsActive(isActive);
        return this;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Department getLockedDepartment() {
        return lockedDepartment;
    }

    public void setLockedDepartment(Department lockedDepartment) {
        this.lockedDepartment = lockedDepartment;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public Boolean getIsVip() {
        return isVip;
    }

    public void setIsVip(Boolean isVip) {
        this.isVip = isVip;
    }

    public String getVipAmenities() {
        return vipAmenities;
    }

    public void setVipAmenities(String vipAmenities) {
        this.vipAmenities = vipAmenities;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Room)) {
            return false;
        }
        return getId() != null && getId().equals(((Room) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Room{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", capacity=" + getCapacity() +
            ", isActive='" + getIsActive() + "'" +
            "}";
    }
}
