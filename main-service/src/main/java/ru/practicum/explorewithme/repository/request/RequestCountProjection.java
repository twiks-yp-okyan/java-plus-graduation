package ru.practicum.explorewithme.repository.request;

public interface RequestCountProjection {
    Long getEventId();

    Long getConfirmedRequestsAmount();
}