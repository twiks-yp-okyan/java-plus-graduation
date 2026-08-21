package ru.yandex.practicum.ewm.analyzer.dto;

import org.mapstruct.Mapper;
import ru.yandex.practicum.ewm.analyzer.model.Interaction;

@Mapper(componentModel = "spring")
public interface InteractionMapper {
    InteractionDto toDto(Interaction entity);
}
