package ru.yandex.practicum.ewm.collector.config;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.VoidSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.ewm.collector.util.AvroCommonSerializer;

import java.util.Properties;

@Configuration
public class KafkaConfig {
    @Value("${kafka.bootstrap-servers}")
    private String KAFKA_BOOTSTRAP_SERVERS;

    @Bean
    public Producer<Void, SpecificRecord> producer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, VoidSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroCommonSerializer.class);

        return new KafkaProducer<>(config);
    }
}
