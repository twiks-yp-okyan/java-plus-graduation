package ru.yandex.practicum.ewm.analyzer.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.ewm.analyzer.model.Interaction;

@Mapper(componentModel = "spring")
public interface InteractionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    InteractionDto toDto(Interaction entity);
}
