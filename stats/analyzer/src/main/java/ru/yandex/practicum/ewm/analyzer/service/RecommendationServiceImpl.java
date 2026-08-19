package ru.yandex.practicum.ewm.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.ewm.analyzer.dto.EventScore;
import ru.yandex.practicum.ewm.analyzer.dto.EventSimilarity;
import ru.yandex.practicum.ewm.analyzer.model.Interaction;
import ru.yandex.practicum.ewm.analyzer.model.Similarity;
import ru.yandex.practicum.ewm.analyzer.repository.InteractionRepository;
import ru.yandex.practicum.ewm.analyzer.repository.SimilarityRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final InteractionRepository interactionRepository;
    private final SimilarityService similarityService;
    private final SimilarityRepository similarityRepository;
    private final Integer N = 20;
    private final Integer K = 10;
    private final Double ZERO_DOUBLE = 0.0;

    @Override
    public List<EventScore> getRecommendedEventsForUser(Long userId, Integer size) {
        if (getUserLastInteractedEvents(userId, N).isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> lastInteractedEvents = getUserLastInteractedEvents(userId, N);
        List<Long> newMostSimilarEvents = getNewMostSimilarEvents(lastInteractedEvents, userId);

        return newMostSimilarEvents.stream()
                .map(id -> calcEventScore(id, userId))
                .sorted(Comparator.comparing(EventScore::score, Comparator.reverseOrder()))
                .limit(size)
                .toList();
    }

    private EventScore calcEventScore(Long eventId, Long userId) {
        /*
        * метод рассчитывает рекомендательную оценку для мероприятия,
        * с которым пользователь еще не взаимодействовал,
        * на основе его схожести с мероприятиями, с которыми он уже взаимодействовал
        * */
        List<Long> userLastInteractedEvents = getUserLastInteractedEvents(userId, N);
        List<EventSimilarity> mostSimilarInteractedEvents = similarityRepository.findByEvent1IdOrEvent2Id(eventId, eventId)
                .stream()
                .filter(s -> isPairFullyInteracted(userLastInteractedEvents, s))
                .sorted(Comparator.comparing(Similarity::getSimilarity, Comparator.reverseOrder()))
                .map(s -> new EventSimilarity(
                        eventId.equals(s.getEvent1Id()) ? s.getEvent2Id() : s.getEvent1Id()
                        , s.getSimilarity()
                        )
                )
                .limit(K)
                .toList();

        Map<Long, Double> mostSimilarEventsScores = mostSimilarInteractedEvents.stream()
                .map(event -> interactionRepository.findByUserIdAndEventId(userId, event.eventId()))
                .map(interaction -> interaction.orElse(null))
                .collect(Collectors.toMap(Interaction::getEventId, Interaction::getRating));

        Double sumWeightedScores = mostSimilarInteractedEvents.stream()
                .map(event -> event.similarity() * mostSimilarEventsScores.get(event.eventId()))
                .reduce(ZERO_DOUBLE, Double::sum);
        Double sumSimilarities = mostSimilarInteractedEvents.stream()
                .map(EventSimilarity::similarity)
                .reduce(ZERO_DOUBLE, Double::sum);

        return new EventScore(eventId, sumWeightedScores / sumSimilarities);
    }

    private List<Long> getNewMostSimilarEvents(List<Long> eventIds, Long userId) {
        /*
        * метод для получения мероприятий, с которыми пользователь не взаимодействовал,
        * но которые наиболее схожие с мероприятиями, с которыми он взаимодействовал
        * для будущего предсказания оценки
        * */

        Set<EventSimilarity> newSimilarEvents = new HashSet<>();
        for (Long eventId : eventIds) {
            newSimilarEvents.addAll(getSimilarEventsNewForUser(userId, eventId, N));
        }

        return new ArrayList<>(newSimilarEvents).stream()
                .sorted(Comparator.comparing(EventSimilarity::similarity, Comparator.reverseOrder()))
                .map(EventSimilarity::eventId)
                .toList();
    }

    private List<EventSimilarity> getSimilarEventsNewForUser(Long userId, Long eventId, Integer size) {
        return similarityService.getSimilarEventsForUser(userId, eventId, size).stream()
                .map(s -> new EventSimilarity(
                        eventId.equals(s.event1Id()) ? s.event2Id() : s.event1Id(),
                        s.similarity()
                ))
                .toList();

    }

    private List<Long> getUserLastInteractedEvents(Long userId, Integer size) {
        return interactionRepository.findByUserIdOrderByLastUpdatedAtDesc(userId).stream()
                .map(Interaction::getEventId)
                .limit(size)
                .toList();
    }

    private boolean isPairFullyInteracted(List<Long> interactedIds, Similarity pair) {
        return interactedIds.contains(pair.getEvent1Id()) && interactedIds.contains(pair.getEvent2Id());
    }
}
