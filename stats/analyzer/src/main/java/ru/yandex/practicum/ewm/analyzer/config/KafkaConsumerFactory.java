package ru.yandex.practicum.ewm.analyzer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.VoidDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.analyzer.utils.EventSimilarityEventDeserializer;
import ru.yandex.practicum.ewm.analyzer.utils.UserActionEventDeserializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerFactory {
    private final KafkaProperties kafkaProperties;

    @Bean
    public Map<String, KafkaConsumer<Void, SpecificRecord>> consumers() {
        Map<String, KafkaConsumer<Void, SpecificRecord>> createdConsumers = new HashMap<>();

        kafkaProperties.getConsumers().forEach((name, consumerProperties) -> {
            log.info("Creating consumer: name={}, topic={}, group={}",
                    name, consumerProperties.getTopic(), consumerProperties.getGroupId());

            KafkaConsumer<Void, SpecificRecord> consumer = createConsumer(consumerProperties);
            createdConsumers.put(name, consumer);
        });

        return createdConsumers;
    }

    private KafkaConsumer<Void, SpecificRecord> createConsumer(ConsumerProperties consumerProperties) {
        Properties properties = buildBaseProperties(consumerProperties);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, resolveDeserializer(consumerProperties.getDeserializer()));

        KafkaConsumer<Void, SpecificRecord> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(List.of(consumerProperties.getTopic()));

        return consumer;
    }

    private Class<?> resolveDeserializer(ConsumerProperties.DeserializerType type) {
        return switch (type) {
            case USER_ACTION -> UserActionEventDeserializer.class;
            case EVENT_SIMILARITY -> EventSimilarityEventDeserializer.class;
        };
    }

    private Properties buildBaseProperties(ConsumerProperties consumerProperties) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getAutoOffsetReset());
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerProperties.getMaxPollRecords());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerProperties.getGroupId());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, VoidDeserializer.class);
        return properties;
    }
}
