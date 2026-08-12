package ru.practicum.explorewithme.mapper;

import ru.practicum.explorewithme.dto.location.LocationDto;
import ru.practicum.explorewithme.model.location.Location;

public final class LocationMapper {
    public static Location mapToLocation(LocationDto dto) {
        return Location.builder()
                .lon(dto.getLon().floatValue())
                .lat(dto.getLat().floatValue())
                .build();
    }

    public static LocationDto mapToDto(Location location) {
        return LocationDto.builder()
                .lon(Double.valueOf(location.getLon()))
                .lat(Double.valueOf(location.getLat()))
                .build();
    }

    public static Location copy(Location location) {
        if (location == null) {
            return null;
        }
        return Location.builder()
                .id(location.getId())
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}
