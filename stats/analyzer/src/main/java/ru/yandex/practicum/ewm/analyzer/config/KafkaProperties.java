package ru.yandex.practicum.ewm.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
public class KafkaProperties {
    private String bootstrapServers;
    private String autoOffsetReset;
    private Map<String, ConsumerProperties> consumers = new HashMap<>();
}