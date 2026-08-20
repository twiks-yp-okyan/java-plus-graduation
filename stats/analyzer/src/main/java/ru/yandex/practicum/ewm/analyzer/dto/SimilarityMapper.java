package ru.yandex.practicum.ewm.analyzer.dto;

import org.mapstruct.Mapper;
import ru.yandex.practicum.ewm.analyzer.model.Similarity;

@Mapper(componentModel = "spring")
public interface SimilarityMapper {
    SimilarityDto toDto(Similarity entity);
}
