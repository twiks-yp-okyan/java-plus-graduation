package ru.yandex.practicum.ewm.collector.service;

import ru.yandex.practicum.grpc.message.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto event);
}
