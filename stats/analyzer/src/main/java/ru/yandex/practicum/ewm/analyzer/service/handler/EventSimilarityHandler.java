package ru.yandex.practicum.ewm.analyzer.service.handler;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface EventSimilarityHandler {
    void handle(EventSimilarityAvro event);
}
