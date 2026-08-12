package ru.practicum.explorewithme.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Status {
    PENDING, CONFIRMED, REJECTED, CANCELED;

    @JsonCreator
    public static Status from(String value) {
        for (Status status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status=" + value);
    }
}
