package ru.yandex.practicum.ewm.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.ewm.analyzer.dto.InteractionDto;
import ru.yandex.practicum.ewm.analyzer.dto.InteractionMapper;
import ru.yandex.practicum.ewm.analyzer.model.projection.EventMaxRating;
import ru.yandex.practicum.ewm.analyzer.repository.InteractionRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {
    private final InteractionRepository repository;
    private final InteractionMapper interactionMapper;

    @Override
    public List<InteractionDto> getEventsByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(interactionMapper::toDto)
                .toList();
    }

    @Override
    public List<EventMaxRating> getEventsMaxRating(List<Long> eventIds) {
        return repository.getEventsMaxRating(eventIds);
    }
}
