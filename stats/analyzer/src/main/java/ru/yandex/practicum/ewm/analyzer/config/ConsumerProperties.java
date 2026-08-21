package ru.yandex.practicum.ewm.analyzer.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsumerProperties {
    private String topic;
    private String groupId;
    private Integer maxPollRecords;
    private DeserializerType deserializer;

    public enum DeserializerType {
        USER_ACTION,
        EVENT_SIMILARITY
    }
}