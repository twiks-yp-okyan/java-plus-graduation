package ru.yandex.practicum.ewm.analyzer.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.ewm.analyzer.model.Similarity;

@Mapper(componentModel = "spring")
public interface SimilarityMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    SimilarityDto toDto(Similarity entity);
}
