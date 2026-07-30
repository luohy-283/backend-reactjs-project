package com.company.bookingroom.service.mapper;

import com.company.bookingroom.domain.Room;
import com.company.bookingroom.service.dto.RoomDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Room} and its DTO {@link RoomDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomMapper extends EntityMapper<RoomDTO, Room> {}
