package ru.yandex.practicum.request.repository;

public interface RequestCountProjection {
    Long getEventId();

    Long getConfirmedRequestsAmount();
}