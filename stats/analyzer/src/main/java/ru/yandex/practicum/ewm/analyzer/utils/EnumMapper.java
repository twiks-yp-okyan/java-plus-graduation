package ru.yandex.practicum.ewm.analyzer.utils;

public final class EnumMapper {

    private EnumMapper() {
    }

    public static <
            S extends Enum<S>,
            T extends Enum<T>
            > T map(S source, Class<T> targetClass) {

        if (source == null) {
            return null;
        }

        return Enum.valueOf(targetClass, source.name());
    }
}