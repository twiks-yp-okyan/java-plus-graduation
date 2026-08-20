package ru.yandex.practicum.ewm.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.ewm.analyzer.model.Interaction;
import ru.yandex.practicum.ewm.analyzer.model.projection.EventMaxRating;

import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);

    List<Interaction> findByUserId(Long userId);

    List<Interaction> findByUserIdOrderByLastUpdatedAtDesc(Long userId);

    @Query(value = "SELECT i.eventId as eventId, SUM(i.rating) as rating "
    + "FROM Interaction i "
    + "WHERE i.eventId IN (:eventIds) "
    + "GROUP BY i.eventId")
    List<EventMaxRating> getEventsMaxRating(@Param("eventIds") List<Long> eventIds);
}
