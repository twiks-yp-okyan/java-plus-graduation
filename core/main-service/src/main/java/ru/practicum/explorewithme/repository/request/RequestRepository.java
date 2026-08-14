package ru.practicum.explorewithme.repository.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.model.request.Request;

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
            "WHERE r.event.id = :eventId")
    Long countRequestsByEventId(@Param("eventId") Long eventId);

    @Query(value = "SELECT r.event_id AS eventId, COUNT(r.id) AS confirmedRequestsAmount " +
            "FROM requests r " +
            "WHERE r.event_id IN (:eventIds) AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event_id",
            nativeQuery = true)
    List<RequestCountProjection> countConfirmedRequestsByEventIds(@Param("eventIds") Set<Long> eventIds);

    @Query("SELECT COUNT(r) " +
            "FROM Request r " +
            "WHERE r.event.id = :eventId AND r.status = ru.practicum.explorewithme.model.request.Status.CONFIRMED")
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    List<Request> findRequestsByRequesterId(Long id);

    Optional<Request> findByRequesterIdAndEventId(Long requesterId, Long eventId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByIdInAndEventId(List<Long> requestIds, Long eventId);

    List<Request> findAllByEventIdAndStatus(Long eventId, ru.practicum.explorewithme.model.request.Status status);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :userId")
    boolean existsUserById(Long userId);

    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE e.id = :eventId")
    boolean existsEventById(Long eventId);
}
