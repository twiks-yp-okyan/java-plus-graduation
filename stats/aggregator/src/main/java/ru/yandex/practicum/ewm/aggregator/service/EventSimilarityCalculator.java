package ru.yandex.practicum.ewm.aggregator.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;

public interface EventSimilarityCalculator {
    List<EventSimilarityAvro> calculateSimilarityAfterUserAction(UserActionAvro event);
}
