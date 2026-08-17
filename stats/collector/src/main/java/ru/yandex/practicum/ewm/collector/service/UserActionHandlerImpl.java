package ru.yandex.practicum.ewm.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.ewm.collector.util.EnumMapper;
import ru.yandex.practicum.grpc.recommendation.message.user.action.UserActionProto;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActionHandlerImpl implements UserActionHandler {
    private final KafkaEventProducer kafkaEventProducer;
    @Value("${kafka.producer.user-actions}")
    private String KAFKA_TOPIC_NAME;

    @Override
    public void handle(UserActionProto event) {
        log.debug("Попытка конвертации proto -> avro для UserActionProto от пользователя = {} для события - {}",
                event.getUserId(),
                event.getEventId()
        );
        UserActionAvro userActionAvro = mapToAvro(event);
        log.debug("Отправляем avro-сообщение в топик {} с данными - {}",  KAFKA_TOPIC_NAME, userActionAvro);
        kafkaEventProducer.send(KAFKA_TOPIC_NAME, userActionAvro);
    }

    private UserActionAvro mapToAvro(UserActionProto event) {
        return UserActionAvro.newBuilder()
                .setUserId(event.getUserId())
                .setEventId(event.getEventId())
                .setActionType(EnumMapper.map(event.getActionType(), ActionTypeAvro.class))
                .setTimestamp(Instant.ofEpochSecond(event.getTimestamp().getSeconds(), event.getTimestamp().getNanos()))
                .build();
    }
}
