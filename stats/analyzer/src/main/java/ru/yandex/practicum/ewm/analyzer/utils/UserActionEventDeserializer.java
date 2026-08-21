package ru.yandex.practicum.ewm.analyzer.utils;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionEventDeserializer extends AvroCommonDeserializer<UserActionAvro> {
    public UserActionEventDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}
