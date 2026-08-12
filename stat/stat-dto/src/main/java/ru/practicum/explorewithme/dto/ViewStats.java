package ru.practicum.explorewithme.dto;

public record ViewStats(
        String app,
        String uri,
        Long hits
) {
}
