package ru.yandex.practicum.ewm.aggregator.service;

import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventSimilarityCalculatorImpl implements EventSimilarityCalculator {
    private final Map<Long, Map<Long, Double>> eventsWeightsByUserAction = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> calculateSimilarityAfterUserAction(UserActionAvro event) {
        EventSimilarityAvro eventSimilarity = EventSimilarityAvro.newBuilder()
                .setEventA(event.getEventId())
                .setEventB(1L)
                .setScore(0.66)
                .build();
        return List.of(eventSimilarity);
    }

    private void processAndSaveUserAction(UserActionAvro event, Map<Long, Map<Long, Double>> map) {
        Double currentUserActionWeight = map.get(event.getEventId()).get(event.getUserId());
    }
}
