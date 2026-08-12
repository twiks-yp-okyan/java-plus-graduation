package ru.practicum.explorewithme.model;

import java.time.LocalDateTime;

public record HitEntity(
        Long id,
        String app,
        String uri,
        String ip,
        LocalDateTime created
) {
}
