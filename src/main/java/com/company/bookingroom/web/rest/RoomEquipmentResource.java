package com.company.bookingroom.web.rest;

import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.RoomEquipmentService;
import com.company.bookingroom.service.dto.RoomEquipmentDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomId}/equipment")
public class RoomEquipmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoomEquipmentResource.class);

    private final RoomEquipmentService roomEquipmentService;

    public RoomEquipmentResource(RoomEquipmentService roomEquipmentService) {
        this.roomEquipmentService = roomEquipmentService;
    }

    @GetMapping("")
    public List<RoomEquipmentDTO> getRoomEquipment(@PathVariable Long roomId) {
        LOG.debug("REST request to get equipment for room {}", roomId);
        return roomEquipmentService.findByRoom(roomId);
    }

    @PutMapping("/{roomEquipmentId}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public ResponseEntity<RoomEquipmentDTO> update(
        @PathVariable Long roomId,
        @PathVariable Long roomEquipmentId,
        @RequestBody RoomEquipmentDTO dto
    ) {
        LOG.debug("REST request to update room equipment {}/{}", roomId, roomEquipmentId);
        return ResponseEntity.ok(roomEquipmentService.update(roomId, roomEquipmentId, dto));
    }

    @PatchMapping("/{roomEquipmentId}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.STAFF + "\")")
    public ResponseEntity<RoomEquipmentDTO> patch(
        @PathVariable Long roomId,
        @PathVariable Long roomEquipmentId,
        @RequestBody RoomEquipmentDTO dto
    ) {
        LOG.debug("REST request to patch room equipment {}/{}", roomId, roomEquipmentId);
        return ResponseEntity.ok(roomEquipmentService.update(roomId, roomEquipmentId, dto));
    }

    @PostMapping("/{roomEquipmentId}/report-broken")
    public ResponseEntity<RoomEquipmentDTO> reportBroken(@PathVariable Long roomId, @PathVariable Long roomEquipmentId) {
        LOG.debug("REST request to report broken room equipment {}/{}", roomId, roomEquipmentId);
        return ResponseEntity.ok(roomEquipmentService.reportBroken(roomId, roomEquipmentId));
    }
}
