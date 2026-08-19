package ru.yandex.practicum.ewm.analyzer.service;

import ru.yandex.practicum.ewm.analyzer.dto.SimilarityDto;

import java.util.List;

public interface SimilarityService {
    List<SimilarityDto> getSimilarEventsForUser(Long userId, Long eventId, Integer size);
}
