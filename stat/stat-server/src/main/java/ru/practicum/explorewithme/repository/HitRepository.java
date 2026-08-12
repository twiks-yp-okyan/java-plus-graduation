package ru.practicum.explorewithme.repository;

import ru.practicum.explorewithme.dto.ViewStats;
import ru.practicum.explorewithme.model.HitEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository {
    HitEntity save(HitEntity hit);

    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
