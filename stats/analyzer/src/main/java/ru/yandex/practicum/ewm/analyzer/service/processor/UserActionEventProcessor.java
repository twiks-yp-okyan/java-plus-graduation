package ru.yandex.practicum.ewm.analyzer.service.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.ewm.analyzer.service.handler.UserActionHandler;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionEventProcessor {
    private final Map<String, KafkaConsumer<Void, SpecificRecord>> consumers;
    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private final UserActionHandler handler;

    public void start() {
        KafkaConsumer<Void, SpecificRecord> consumer = consumers.get("user-actions");

        try {
            while (true) {
                ConsumerRecords<Void, SpecificRecord> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                if (!records.isEmpty()) {
                    log.debug("Получено {} сообщений за один poll из топика - stats.user-actions.v1", records.count());
                }
                for (ConsumerRecord<Void, SpecificRecord> record : records) {
                    log.debug("USER_ACTION Event with: offset - {}, value - {}", record.offset(), record.value());

                    UserActionAvro event = (UserActionAvro) record.value();
                    handler.handle(event);
                    log.debug("Действие пользователя {} для события {} сохранено", event.getUserId(), event.getEventId());
                }
                consumer.commitAsync();
            }
        } catch (Exception e) {
            log.error("Ошибка во время обработки снапшота", e);
        } finally {
            try {
                consumer.commitSync();
                log.debug("Snapshot-Consumer make commit Sync method");
            } finally {
                log.info("Закрываем консьюмер-user-actions");
                consumer.close();
            }
        }
    }
}
