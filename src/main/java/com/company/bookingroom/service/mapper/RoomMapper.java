package com.company.bookingroom.service.mapper;

import com.company.bookingroom.domain.Room;
import com.company.bookingroom.service.dto.RoomDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity {@link Room} and its DTO {@link RoomDTO}.
 */
@Mapper(componentModel = "spring", uses = { DepartmentMapper.class })
public interface RoomMapper extends EntityMapper<RoomDTO, Room> {}
