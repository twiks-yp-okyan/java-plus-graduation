package ru.yandex.practicum.ewm.analyzer.dto;

public record SimilarityDto(
        Long event1Id,
        Long event2Id,
        Double similarity
) {}
