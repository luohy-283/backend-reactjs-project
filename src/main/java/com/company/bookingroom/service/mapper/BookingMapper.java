package com.company.bookingroom.service.mapper;

import com.company.bookingroom.domain.Booking;
import com.company.bookingroom.domain.Room;
import com.company.bookingroom.domain.User;
import com.company.bookingroom.service.dto.BookingDTO;
import com.company.bookingroom.service.dto.RoomDTO;
import com.company.bookingroom.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Booking} and its DTO {@link BookingDTO}.
 */
@Mapper(componentModel = "spring")
public interface BookingMapper extends EntityMapper<BookingDTO, Booking> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomName")
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    BookingDTO toDto(Booking s);

    @Named("roomName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "pricePerHour", source = "pricePerHour")
    RoomDTO toDtoRoomName(Room room);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
