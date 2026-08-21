package ru.yandex.practicum.ewm.analyzer.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.ewm.analyzer.model.Similarity;
import ru.yandex.practicum.ewm.analyzer.repository.SimilarityRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSimilarityHandlerImpl implements EventSimilarityHandler {
    private final SimilarityRepository repository;

    @Override
    public void handle(EventSimilarityAvro event) {
        Similarity similarity;
        Map<String, Long> sortedEvents = sortEvents(event.getEventA(), event.getEventB());
        Optional<Similarity> currentSimilarity = repository.findByEvent1IdAndEvent2Id(
                sortedEvents.get("eventA"),
                sortedEvents.get("eventB")
        );
        if (currentSimilarity.isPresent()) {
            similarity = currentSimilarity.get();
            log.debug("Попытка обновить значение similarity для eventA - {} & eventB - {}",
                    sortedEvents.get("eventA"), sortedEvents.get("eventB"));
            if (Double.compare(similarity.getSimilarity(), event.getScore()) != 0) {
                similarity.setSimilarity(event.getScore());
            }
        } else {
            log.debug("Новый Similarity для eventA - {} & eventB - {}",
                    sortedEvents.get("eventA"), sortedEvents.get("eventB"));
            similarity = Similarity.builder()
                    .event1Id(sortedEvents.get("eventA"))
                    .event2Id(sortedEvents.get("eventB"))
                    .similarity(event.getScore())
                    .build();
        }
        repository.save(similarity);
    }

    private Map<String, Long> sortEvents(Long event1Id, Long event2Id) {
        Map<String, Long> sortedEvents = new HashMap<>();
        sortedEvents.put("eventA", Math.min(event1Id, event2Id));
        sortedEvents.put("eventB", Math.max(event1Id, event2Id));
        return sortedEvents;
    }
}
