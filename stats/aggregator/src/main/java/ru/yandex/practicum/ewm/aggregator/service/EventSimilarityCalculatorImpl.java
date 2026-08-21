package ru.yandex.practicum.ewm.aggregator.service;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

        log.debug("Отображение с весами пользователей для мероприятий содержит ключ - {}: {}",
                eventId, eventUserWeight.containsKey(eventId));
        eventUserWeight.putIfAbsent(eventId, new HashMap<>());
        Map<Long, Double> eventWeights = eventUserWeight.get(eventId);

        Double newWeight = mapRating(event.getActionType());

        if (!eventWeights.containsKey(userId)) {
            log.debug("Первое взаимодействие пользователя - {} с мероприямием - {}. Добавляем его оценку сумме оценок.",
                    userId, eventId);
            eventTotalWeight.merge(eventId, newWeight, Double::sum);
        }

        eventWeights.putIfAbsent(userId, newWeight);

        Double currentWeight = eventWeights.get(userId);
        if (!needUpdateWeight(currentWeight, newWeight)) {
            log.debug("Для события - {} у пользователя {} новая оценка {} не отличается от старой - {}", eventId, userId, newWeight, currentWeight);
            return new ArrayList<>();
        }
        eventWeights.put(userId, newWeight);
        // обновляем суммы
        Double weightDelta = newWeight - currentWeight;
        log.debug("Разница между оценками для события {} от пользователя {} - {}", eventId, userId, weightDelta);
        // currentWeight по дефолту, так как если сумма пустая, то первое значение - вес текущего мероприятия
        eventTotalWeight.merge(eventId, weightDelta, Double::sum);
        log.debug("Обновили сумму оценок для события {}", eventId);

        log.debug("Начало пересчета коэффициентов");
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeight.entrySet()) {
            Long eventForCompareId = entry.getKey();

            if (eventForCompareId.equals(eventId)) {
                continue;
            }

            Map<Long, Double> comparingEventUserWeights = entry.getValue();
            log.debug("Пользователь - {} взаимодействовал с событием для сравнения - {}: {}",
                    userId, eventForCompareId, comparingEventUserWeights.containsKey(userId));
            if (comparingEventUserWeights.containsKey(userId)) {
                log.debug("Пересчет коэффициента между событиями {} и {}", eventId, eventForCompareId);
                Double comparingEventUserWeight = comparingEventUserWeights.get(userId);
                log.debug("Оценка пользователя событию {} для сравнения - {}", eventForCompareId, comparingEventUserWeight);

                Double oldMinContribution = Math.min(currentWeight, comparingEventUserWeight);
                Double newMinContribution = Math.min(newWeight, comparingEventUserWeight);
                Double deltaContribution = newMinContribution - oldMinContribution;

                log.debug("oldMinContribution = {}, newMinContribution - {}", oldMinContribution, newMinContribution);

                Long eventA = Math.min(eventId, eventForCompareId);
                Long eventB = Math.max(eventId, eventForCompareId);

                eventPairsMinSum.putIfAbsent(eventA, new HashMap<>());
                eventPairsMinSum.get(eventA).merge(eventB, deltaContribution, Double::sum);

                Double sMin = eventPairsMinSum.get(eventA).get(eventB);
                Double sumEventA = eventTotalWeight.get(eventA);
                Double sumEventB = eventTotalWeight.get(eventB);
                log.debug("Числитель - {}, знаменатель для А - {}, знаменатель для B - {}", sMin, sumEventA, sumEventB);

                double score = 0.0;
                if (sumEventA > 0 && sumEventB > 0) {
                    log.debug("Переситываес коэффициент");
                    score = sMin / (Math.sqrt(sumEventA) * Math.sqrt(sumEventB));
                }
                log.debug("Новый коэффициент - {}", score);

                EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                        .setEventA(eventA)
                        .setEventB(eventB)
                        .setScore(score)
                        .setTimestamp(event.getTimestamp())
                        .build();
                log.debug("Добавляем новый объект для ответа с параметрами: eventA - {}, eventB = {}, score - {}",
                        eventA, eventB, score);
                eventSimilarities.add(similarity);
            }
        }
        log.debug("Конец пересчета коэффициентов. Пересчитано - {}", eventSimilarities.size());
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
