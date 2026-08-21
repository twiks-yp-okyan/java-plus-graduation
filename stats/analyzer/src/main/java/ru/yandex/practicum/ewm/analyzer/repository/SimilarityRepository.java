package ru.yandex.practicum.ewm.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.ewm.analyzer.model.Similarity;

import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
    Optional<Similarity> findByEvent1IdAndEvent2Id(Long event1Id, Long event2Id);

    List<Similarity> findByEvent1IdOrEvent2Id(Long event1Id, Long event2Id);
}
