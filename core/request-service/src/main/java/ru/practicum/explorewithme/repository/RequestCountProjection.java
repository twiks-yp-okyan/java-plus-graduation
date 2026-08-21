package ru.practicum.explorewithme.repository;

public interface RequestCountProjection {
    Long getEventId();

    Long getConfirmedRequestsAmount();
}