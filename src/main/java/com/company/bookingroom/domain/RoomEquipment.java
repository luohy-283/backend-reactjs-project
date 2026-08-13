package com.company.bookingroom.domain;

import com.company.bookingroom.domain.enumeration.RoomEquipmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Per-room inventory row for a catalog equipment item.
 */
@Entity
@Table(
    name = "room_equipment",
    uniqueConstraints = @UniqueConstraint(name = "ux_room_equipment_room_equipment", columnNames = { "room_id", "equipment_id" })
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class RoomEquipment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @NotNull
    @Min(0)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RoomEquipmentStatus status = RoomEquipmentStatus.OK;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public RoomEquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(RoomEquipmentStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomEquipment)) {
            return false;
        }
        return id != null && id.equals(((RoomEquipment) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
