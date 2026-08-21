package ru.yandex.practicum.ewm.analyzer.service;

import ru.yandex.practicum.ewm.analyzer.dto.EventScore;

import java.util.List;

public interface RecommendationService {
    List<EventScore> getRecommendedEventsForUser(Long userId, Integer size);
}
