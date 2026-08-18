package ru.yandex.practicum.ewm.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.ewm.aggregator.service.EventSimilarityCalculator;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregationStarter {
    private final Producer<Void, SpecificRecord> producer;
    private final Consumer<Void, UserActionAvro> consumer;
    private final EventSimilarityCalculator eventSimilarityCalculator;

    @Value("${kafka.consumer.topic}")
    private String KAFKA_USER_ACTION_TOPIC;
    @Value("${kafka.producer.topic}")
    private String KAFKA_EVENTS_SIMILARITY_TOPIC;

    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    /**
     * Метод для начала процесса агрегации данных.
     * Подписывается на топики для получения событий от датчиков,
     * формирует снимок их состояния и записывает в кафку.
     */
    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {

            // ... подготовка к обработке данных ...
            // ... например, подписка на топик ...
            consumer.subscribe(List.of(KAFKA_USER_ACTION_TOPIC));

            // Цикл обработки событий
            while (true) {
                // ... реализация цикла опроса ...
                // ... и обработка полученных данных ...
                ConsumerRecords<Void, UserActionAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                if (records.count() > 0) {
                    log.debug("Получено {} сообщений за один poll", records.count());
                }
                for (ConsumerRecord<Void, UserActionAvro> record : records) {
                    log.debug("Event with: topic - {}, offset - {}, value - {}", record.topic(), record.offset(), record.value());
                    List<EventSimilarityAvro> recalculatedEventsSimilarities = eventSimilarityCalculator.calculateSimilarityAfterUserAction(record.value());
                    if (!recalculatedEventsSimilarities.isEmpty()) {
                        log.debug("Коэффициенты схожести пересчитаны");
                        for (EventSimilarityAvro similarity : recalculatedEventsSimilarities) {
                            ProducerRecord<Void, SpecificRecord> producerRecord = new ProducerRecord<>(KAFKA_EVENTS_SIMILARITY_TOPIC, similarity);
                            producer.send(producerRecord);
                            log.debug("Коэффициент схожести между событиями {} и {} отправлен в кафку", similarity.getEventA(), similarity.getEventB());
                        }
                    }
                }
                consumer.commitAsync();
            }

        } catch (WakeupException ignored) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий после действий пользователей", e);
        } finally {

            try {
                // Перед тем, как закрыть продюсер и консьюмер, нужно убедиться,
                // что все сообщения, лежащие в буффере, отправлены и
                // все оффсеты обработанных сообщений зафиксированы

                // здесь нужно вызвать метод продюсера для сброса данных в буффере
                producer.flush();
                log.debug("Produces flushes...");
                // здесь нужно вызвать метод консьюмера для фиксации смещений
                consumer.commitSync();
                log.debug("Consumer make commit Sync method");

            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }
}
