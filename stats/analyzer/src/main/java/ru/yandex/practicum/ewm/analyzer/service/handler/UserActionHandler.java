package ru.yandex.practicum.ewm.analyzer.service.handler;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionHandler {
    void handle(UserActionAvro event);
}
