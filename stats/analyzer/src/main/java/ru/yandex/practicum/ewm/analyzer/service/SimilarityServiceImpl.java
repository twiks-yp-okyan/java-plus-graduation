package ru.yandex.practicum.ewm.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.ewm.analyzer.dto.InteractionDto;
import ru.yandex.practicum.ewm.analyzer.dto.SimilarityDto;
import ru.yandex.practicum.ewm.analyzer.dto.SimilarityMapper;
import ru.yandex.practicum.ewm.analyzer.model.Similarity;
import ru.yandex.practicum.ewm.analyzer.repository.SimilarityRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimilarityServiceImpl implements SimilarityService {
    private final SimilarityRepository repository;
    private final InteractionService interactionService;
    private final SimilarityMapper similarityMapper;

    @Override
    public List<SimilarityDto> getSimilarEventsForUser(Long userId, Long eventId, Integer size) {
        List<Similarity> eventPairs = repository.findByEvent1IdOrEvent2Id(eventId, eventId);
        List<Long> userInteractionsEventIds = interactionService.getEventsByUserId(userId).stream()
                .map(InteractionDto::eventId)
                .toList();

        return eventPairs.stream()
                .filter(pair -> isPairEventNew(userInteractionsEventIds, pair))
                .sorted(Comparator.comparing(Similarity::getSimilarity, Comparator.reverseOrder()))
                .map(similarityMapper::toDto)
                .limit(size)
                .toList();
    }

    private boolean isPairEventNew(List<Long> interactionIds, Similarity pair) {
        return !interactionIds.contains(pair.getEvent1Id()) && interactionIds.contains(pair.getEvent2Id());
    }
}
