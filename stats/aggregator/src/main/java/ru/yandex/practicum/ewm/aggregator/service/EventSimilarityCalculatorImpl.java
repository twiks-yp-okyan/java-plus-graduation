package ru.yandex.practicum.ewm.aggregator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventSimilarityCalculatorImpl implements EventSimilarityCalculator {
    @Value("${user.action.weight.like}")
    private Double LIKE_WEIGHT;
    @Value("${user.action.weight.register}")
    private Double REGISTER_WEIGHT;
    @Value("${user.action.weight.view}")
    private Double VIEW_WEIGHT;
    // <eventId, < userId, similarity > > - подсказка себе
    private final Map<Long, Map<Long, Double>> eventUserWeight = new HashMap<>();
    // < eventId, sum(rating) by users >
    private final Map<Long, Double> eventTotalWeight = new HashMap<>();
    // < event1Id, <event2Id, minSum> > - event1Id < event2Id
    private final Map<Long, Map<Long, Double>> eventPairsMinSum = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> calculateSimilarityAfterUserAction(UserActionAvro event) {
        Long userId = event.getUserId();
        Long eventId = event.getEventId();

        List<EventSimilarityAvro> eventSimilarities = new ArrayList<>();

        Map<Long, Double> eventWeights = eventUserWeight.getOrDefault(eventId, new HashMap<>());

        Double newWeight = mapRating(event.getActionType());
        eventWeights.putIfAbsent(userId, newWeight);

        Double currentWeight = eventWeights.get(userId);
        if (!needUpdateWeight(currentWeight, newWeight)) {
            // TODO nothing to return
        }
        // обновляем суммы
        Double weightDelta = newWeight - currentWeight;
        // currentWeight по дефолту, так как если сумма пустая, то первое значение - вес текущего мероприятия
        eventTotalWeight.merge(eventId, weightDelta, Double::sum);

        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeight.entrySet()) {
            Long eventForCompareId = entry.getKey();

            if (eventForCompareId.equals(eventId)) {
                continue;
            }

            Map<Long, Double> comparingEventUserWeights = entry.getValue();
            if (comparingEventUserWeights.containsKey(userId)) {
                Double comparingEventUserWeight = comparingEventUserWeights.get(userId);

                Double oldMinContribution = Math.min(currentWeight, comparingEventUserWeight);
                Double newMinContribution = Math.min(newWeight, comparingEventUserWeight);
                Double deltaContribution = newMinContribution - oldMinContribution;

                Long eventA = Math.min(eventId, eventForCompareId);
                Long eventB = Math.max(eventId, eventForCompareId);

                eventPairsMinSum.putIfAbsent(eventA, new HashMap<>());
                eventPairsMinSum.get(eventA).merge(eventB, deltaContribution, Double::sum);

                Double sMin = eventPairsMinSum.get(eventA).get(eventB);
                Double sumEventA = eventTotalWeight.get(eventA);
                Double sumEventB = eventTotalWeight.get(eventB);

                double score = 0.0;
                if (sumEventA > 0 && sumEventB > 0) {
                    score = sMin / (Math.sqrt(sumEventA) * Math.sqrt(sumEventB));
                }

                EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                        .setEventA(eventA)
                        .setEventB(eventB)
                        .setScore(score)
                        .setTimestamp(event.getTimestamp())
                        .build();

                eventSimilarities.add(similarity);
            }
        }
        return eventSimilarities;
    }

    private boolean needUpdateWeight(Double currentWeight, Double newWeight) {
        return Double.compare(newWeight, currentWeight) > 0;
    }

    private Double mapRating(ActionTypeAvro actionType) {
        return switch (actionType) {
            case LIKE -> LIKE_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case VIEW -> VIEW_WEIGHT;
        };
    }
}
