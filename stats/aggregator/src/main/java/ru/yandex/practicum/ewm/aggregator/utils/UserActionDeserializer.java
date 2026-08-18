package ru.yandex.practicum.ewm.aggregator.utils;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionDeserializer extends AvroCommonDeserializer<UserActionAvro> {
    public UserActionDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}
