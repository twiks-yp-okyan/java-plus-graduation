package ru.yandex.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.request.model.Request;
import ru.yandex.practicum.request.model.Status;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RequestRepository extends JpaRepository<Request, Long> {
    @Query(value = "SELECT r.event_id AS eventId, COUNT(r.id) AS confirmedRequestsAmount " +
            "FROM requests r " +
            "WHERE r.event_id IN (:eventIds) " +
            "GROUP BY r.event_id",
            nativeQuery = true)
    List<RequestCountProjection> countRequestsByEventIds(@Param("eventIds") Set<Long> eventIds);

    @Query("SELECT COUNT(r) " +
            "FROM Request r " +
            "WHERE r.eventId = :eventId")
    Integer countRequestsByEventId(@Param("eventId") Long eventId);

    @Query(value = "SELECT r.event_id AS eventId, COUNT(r.id) AS confirmedRequestsAmount " +
            "FROM requests r " +
            "WHERE r.event_id IN (:eventIds) AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event_id",
            nativeQuery = true)
    List<RequestCountProjection> countConfirmedRequestsByEventIds(@Param("eventIds") Set<Long> eventIds);

    @Query("SELECT COUNT(r) " +
            "FROM Request r " +
            "WHERE r.eventId = :eventId AND r.status = 'CONFIRMED'")
    Integer countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    List<Request> findRequestsByRequesterId(Long id);

    Optional<Request> findByRequesterIdAndEventId(Long requesterId, Long eventId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByIdInAndEventId(List<Long> requestIds, Long eventId);

    List<Request> findAllByEventIdAndStatus(Long eventId, Status status);
}
