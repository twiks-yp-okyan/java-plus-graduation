package ru.yandex.practicum.ewm.analyzer.service;

import ru.yandex.practicum.ewm.analyzer.dto.InteractionDto;
import ru.yandex.practicum.ewm.analyzer.model.projection.EventMaxRating;

import java.util.List;

public interface InteractionService {
    List<InteractionDto> getEventsByUserId(Long userId);

    List<EventMaxRating> getEventsMaxRating(List<Long> eventIds);
}
