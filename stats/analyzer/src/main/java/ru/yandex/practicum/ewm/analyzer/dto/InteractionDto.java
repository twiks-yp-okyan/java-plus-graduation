package ru.yandex.practicum.ewm.analyzer.dto;

public record InteractionDto(
        Long userId,
        Long eventId,
        Double rating
) {}
