package ru.yandex.practicum.ewm.analyzer.utils;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityEventDeserializer extends AvroCommonDeserializer<EventSimilarityAvro> {
    public EventSimilarityEventDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}
