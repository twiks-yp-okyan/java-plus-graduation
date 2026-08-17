package ru.yandex.practicum.ewm.collector.service;

import ru.yandex.practicum.grpc.recommendation.message.user.action.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto event);
}
