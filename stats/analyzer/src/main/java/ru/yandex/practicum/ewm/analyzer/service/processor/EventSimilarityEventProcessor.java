package ru.yandex.practicum.ewm.analyzer.service.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.ewm.analyzer.service.handler.EventSimilarityHandler;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class EventSimilarityEventProcessor implements Runnable {
    private final Map<String, KafkaConsumer<Void, SpecificRecord>> consumers;
    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private final EventSimilarityHandler handler;

    public EventSimilarityEventProcessor(Map<String, KafkaConsumer<Void, SpecificRecord>> consumers,
                                         EventSimilarityHandler handler) {
        this.consumers = consumers;
        this.handler = handler;
    }

    @Override
    public void run() {
        KafkaConsumer<Void, SpecificRecord> consumer = consumers.get("event-similarity");

        try {
            while (true) {
                ConsumerRecords<Void, SpecificRecord> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                if (!records.isEmpty()) {
                    log.debug("Получено {} сообщений за один poll из топика - stats.events-similarity.v1", records.count());
                }
                for (ConsumerRecord<Void, SpecificRecord> record : records) {
                    log.debug("EVENT_SIMILARITY Event with: offset - {}, value - {}", record.offset(), record.value());

                    EventSimilarityAvro event = (EventSimilarityAvro) record.value();
                    handler.handle(event);
                    log.debug("Данные о схожести событий {} и {} обновлены", event.getEventA(), event.getEventB());
                }
                consumer.commitAsync();
            }
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий по схожести событий", e);
        } finally {
            try {
                consumer.commitSync();
                log.debug("EventSimilarity-Consumer make commit Sync method");
            } finally {
                log.info("Закрываем консьюмер-EventSimilarity");
                consumer.close();
            }
        }
    }
}
